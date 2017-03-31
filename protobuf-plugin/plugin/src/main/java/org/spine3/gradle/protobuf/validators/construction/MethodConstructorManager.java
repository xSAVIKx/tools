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
import org.spine3.gradle.protobuf.failure.MessageTypeCache;
import org.spine3.gradle.protobuf.failure.fieldtype.FieldType;
import org.spine3.gradle.protobuf.failure.fieldtype.FieldTypeFactory;
import org.spine3.gradle.protobuf.validators.ValidatorMetadata;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.gradle.protobuf.util.GenerationUtils.getJavaFieldName;
import static org.spine3.gradle.protobuf.util.GenerationUtils.isMap;
import static org.spine3.gradle.protobuf.util.GenerationUtils.isRepeated;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getClassName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getValidatorGenericClassName;
import static org.spine3.gradle.protobuf.validators.construction.AbstractMethodConstructor.AbstractMethodConstructorBuilder;
import static org.spine3.gradle.protobuf.validators.construction.AbstractMethodConstructor.CALL_INITIALIZE_IF_NEEDED;

/**
 * The manager for working with method constructors.
 *
 * @author Illia Shepilov
 */
public class MethodConstructorManager {

    private final String javaClass;
    private final String javaPackage;
    private final ClassName builderGenericClassName;
    private final MessageTypeCache messageTypeCache;
    private final DescriptorProto descriptor;

    public MethodConstructorManager(ValidatorMetadata validatorMetadata,
                                    MessageTypeCache messageTypeCache) {
        this.javaClass = validatorMetadata.getJavaClass();
        this.javaPackage = validatorMetadata.getJavaPackage();
        this.descriptor = validatorMetadata.getMsgDescriptor();
        this.messageTypeCache = messageTypeCache;
        final String javaFieldName = getJavaFieldName(descriptor.getName(), false);
        builderGenericClassName = getValidatorGenericClassName(javaPackage,
                                                               messageTypeCache,
                                                               javaFieldName);
    }

    /**
     * Creates the Java methods according to the ProtoBuf message declaration.
     *
     * @return generated methods
     */
    public Collection<MethodSpec> createMethods() {
        final List<MethodSpec> methods = newArrayList();

        methods.add(createPrivateConstructor());
        methods.add(createNewBuilderMethod());
        methods.addAll(createBuildMethods());
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
        final ClassName builderClass = getClassName(javaPackage, javaClass);
        final MethodSpec buildMethod = MethodSpec.methodBuilder("newBuilder")
                                                 .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                 .returns(builderClass)
                                                 .addStatement("return new $T()", builderClass)
                                                 .build();
        return buildMethod;
    }

    private Collection<MethodSpec> createBuildMethods() {
        final List<FieldDescriptorProto> repeatedFieldDescriptors = newArrayList();
        final StringBuilder builder = new StringBuilder("final $T result = $T.newBuilder()");

        for (FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {
            builder.append('.');
            appendPrefixPrefix(builder, fieldDescriptor);
            storeDescriptorIfNeeded(fieldDescriptor, repeatedFieldDescriptors);
            appendMethodCall(builder, fieldDescriptor);
        }
        builder.append(".build()");

        final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("build")
                                                           .addModifiers(Modifier.PUBLIC)
                                                           .returns(builderGenericClassName);
        if (!repeatedFieldDescriptors.isEmpty()) {
            methodBuilder.addStatement(CALL_INITIALIZE_IF_NEEDED);
        }

        final MethodSpec method = methodBuilder.addStatement(builder.toString(),
                                                             builderGenericClassName,
                                                             builderGenericClassName)
                                               .addStatement("return result")
                                               .build();

        if (!repeatedFieldDescriptors.isEmpty()) {
            final List<MethodSpec> methods = newArrayList();
            methods.add(createCheckRepeatedFieldMethod(repeatedFieldDescriptors));
            methods.add(method);
            return methods;
        }

        return Collections.singletonList(method);
    }

    private static StringBuilder appendMethodCall(StringBuilder builder,
                                                  FieldDescriptorProto fieldDescriptor) {
        return builder.append(getJavaFieldName(fieldDescriptor.getName(), true))
                      .append('(')
                      .append(getJavaFieldName(fieldDescriptor.getName(), false))
                      .append(')');
    }

    private StringBuilder appendPrefixPrefix(StringBuilder builder,
                                             FieldDescriptorProto fieldDescriptor) {
        final FieldType fieldType =
                new FieldTypeFactory(descriptor,
                                     messageTypeCache.getCachedTypes()).create(fieldDescriptor);
        return builder.append(fieldType.getSetterPrefix());
    }

    private static List<FieldDescriptorProto> storeDescriptorIfNeeded(
            FieldDescriptorProto fieldDescriptor, List<FieldDescriptorProto> fieldDescriptors) {
        if (isRepeated(fieldDescriptor)) {
            fieldDescriptors.add(fieldDescriptor);
        }
        return fieldDescriptors;
    }

    private static MethodSpec createCheckRepeatedFieldMethod(
            Iterable<FieldDescriptorProto> fieldDescriptors) {
        final MethodSpec.Builder builder = MethodSpec.methodBuilder("createIfNeeded")
                                                     .addModifiers(Modifier.PRIVATE);
        for (FieldDescriptorProto fieldDescriptor : fieldDescriptors) {
            final String javaFieldName = getJavaFieldName(fieldDescriptor.getName(), false);
            if (isMap(fieldDescriptor)) {
                appendMethodBody(builder, javaFieldName, HashMap.class);
            } else {
                appendMethodBody(builder, javaFieldName, ArrayList.class);
            }
        }
        return builder.build();
    }

    private static <T> MethodSpec.Builder appendMethodBody(MethodSpec.Builder builder,
                                                           String javaFieldName,
                                                           Class<T> classToCreate) {
        return builder.beginControlFlow("if(" + javaFieldName + " == null)")
                      .addStatement(javaFieldName + " = new $T<>()", classToCreate)
                      .endControlFlow();
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

    /**
     * A factory for the method constructors.
     */
    private class MethodConstructorFactory {

        /**
         * Returns the concrete method constructor according to
         * the passed {@code FieldDescriptorProto}
         *
         * @param fieldDescriptor the descriptor for the field
         * @param fieldIndex      the index of the field
         * @return the method constructor instance
         */
        private AbstractMethodConstructor getMethodConstructor(FieldDescriptorProto fieldDescriptor,
                                                               int fieldIndex) {
            if (isMap(fieldDescriptor)) {
                return createMethodConstructor(MapFieldMethodConstructor.newBuilder(),
                                               fieldDescriptor,
                                               fieldIndex);
            }
            if (isRepeated(fieldDescriptor)) {
                return createMethodConstructor(RepeatedFieldMethodConstructor.newBuilder(),
                                               fieldDescriptor,
                                               fieldIndex);
            }
            return createMethodConstructor(SingularFieldMethodConstructor.newBuilder(),
                                           fieldDescriptor,
                                           fieldIndex);
        }

        private AbstractMethodConstructor createMethodConstructor(
                AbstractMethodConstructorBuilder builder,
                FieldDescriptorProto dscr,
                int fieldIndex) {
            final FieldType fieldType =
                    new FieldTypeFactory(descriptor, messageTypeCache.getCachedTypes()).create(dscr);
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
