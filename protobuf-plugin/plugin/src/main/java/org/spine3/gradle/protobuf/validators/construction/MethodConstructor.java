/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spine3.gradle.protobuf.validators.construction;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import org.spine3.gradle.protobuf.MessageTypeCache;
import org.spine3.gradle.protobuf.fieldtype.FieldType;
import org.spine3.gradle.protobuf.fieldtype.FieldTypeFactory;
import org.spine3.gradle.protobuf.validators.ValidatorMetadata;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.gradle.protobuf.GenerationUtils.getJavaFieldName;
import static org.spine3.gradle.protobuf.GenerationUtils.isMap;
import static org.spine3.gradle.protobuf.GenerationUtils.isRepeated;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.ADD_ALL_PREFIX;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.CREATE_IF_NEEDED;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.PUT_ALL_PREFIX;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.SETTER_PREFIX;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getBuilderClassName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getValidatorGenericClassName;
import static org.spine3.gradle.protobuf.validators.construction.AbstractMethodConstructor.MethodConstructorBuilder;

/**
 * @author Illia Shepilov
 */
public class MethodConstructor {

    private final String javaClass;
    private final String javaPackage;
    private final ClassName builderGenericClassName;
    private final MessageTypeCache messageTypeCache;
    private final DescriptorProto descriptor;

    public MethodConstructor(ValidatorMetadata validatorMetadata,
                             MessageTypeCache messageTypeCache) {
        this.javaClass = validatorMetadata.getJavaClass();
        this.javaPackage = validatorMetadata.getJavaPackage();
        this.descriptor = validatorMetadata.getMsgDescriptor();
        this.messageTypeCache = messageTypeCache;
        builderGenericClassName = getValidatorGenericClassName(javaPackage,
                                                               messageTypeCache,
                                                               descriptor.getName());
    }

    public Collection<MethodSpec> createMethods() {
        final List<MethodSpec> methods = newArrayList();

        methods.add(createPrivateConstructor());
        methods.add(createNewBuilderMethod());
        methods.add(createBuildMethod());
        methods.addAll(createGeneratedSetters());

        return methods;
    }

    private static MethodSpec createPrivateConstructor() {
        final MethodSpec result = MethodSpec.constructorBuilder()
                                            .addModifiers(Modifier.PRIVATE)
                                            .build();
        return result;
    }

    private MethodSpec createNewBuilderMethod() {
        final ClassName builderClass = getBuilderClassName(javaPackage, javaClass);
        final MethodSpec buildMethod = MethodSpec.methodBuilder("newBuilder")
                                                 .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                 .returns(builderClass)
                                                 .addStatement("return new $T()", builderClass)
                                                 .build();
        return buildMethod;
    }

    private MethodSpec createBuildMethod() {
        final StringBuilder builder = new StringBuilder("final $T result = $T.newBuilder()");
        for (FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {
            builder.append('.');

            if (isMap(fieldDescriptor)) {
                builder.append(PUT_ALL_PREFIX);
            } else if (isRepeated(fieldDescriptor)) {
                builder.append(ADD_ALL_PREFIX);
            } else {
                builder.append(SETTER_PREFIX);
            }

            builder.append(getJavaFieldName(fieldDescriptor.getName(), true))
                   .append('(')
                   .append(getJavaFieldName(fieldDescriptor.getName(), false))
                   .append(')');
        }
        builder.append(".build()");

        final MethodSpec buildMethod = MethodSpec.methodBuilder("build")
                                                 .addModifiers(Modifier.PUBLIC)
                                                 .returns(builderGenericClassName)
                                                 .addStatement(CREATE_IF_NEEDED)
                                                 .addStatement(builder.toString(),
                                                               builderGenericClassName,
                                                               builderGenericClassName)
                                                 .addStatement("return result")
                                                 .build();
        return buildMethod;
    }

    private Collection<MethodSpec> createGeneratedSetters() {
        final MethodConstructorFactory methodConstructorFactory = new MethodConstructorFactory();
        final List<MethodSpec> setters = newArrayList();
        int index = 0;
        for (FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {
            final AbstractMethodConstructor methodConstructor =
                    methodConstructorFactory.getMethodConstructor(fieldDescriptor, index);
            final Collection<MethodSpec> methods = methodConstructor.construct();
            setters.addAll(methods);

            ++index;
        }

        return setters;
    }

    private class MethodConstructorFactory {
        private AbstractMethodConstructor getMethodConstructor(FieldDescriptorProto fieldDescriptor,
                                                               int index) {
            if (isMap(fieldDescriptor)) {
                return createMethodConstructor(MapFieldMethodConstructor.newBuilder(),
                                               fieldDescriptor,
                                               index);
            }
            if (isRepeated(fieldDescriptor)) {
                return createMethodConstructor(RepeatedFieldMethodConstructor.newBuilder(),
                                               fieldDescriptor,
                                               index);
            }
            return createMethodConstructor(SingularFieldMethodConstructor.newBuilder(), fieldDescriptor, index);
        }

        private AbstractMethodConstructor createMethodConstructor(MethodConstructorBuilder builder,
                                                                  FieldDescriptorProto dscr,
                                                                  int fieldIndex) {
            final FieldType fieldType = new FieldTypeFactory(descriptor, messageTypeCache.getCachedTypes()).create(dscr);
            final AbstractMethodConstructor methodConstructor =
                    builder.setFieldDescriptor(dscr)
                           .setFieldType(fieldType)
                           .setFieldIndex(fieldIndex)
                           .setJavaClass(javaClass)
                           .setJavaPackage(javaPackage)
                           .setBuilderGenericClassName(builderGenericClassName)
                           .setMessageTypeCache(messageTypeCache)
                           .build();
            return methodConstructor;
        }
    }

}
