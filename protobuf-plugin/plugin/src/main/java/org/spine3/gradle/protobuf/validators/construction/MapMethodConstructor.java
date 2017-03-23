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

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import org.spine3.gradle.protobuf.MessageTypeCache;
import org.spine3.validate.ConstraintViolationThrowable;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.gradle.protobuf.GenerationUtils.getJavaFieldName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.ADD_RAW_PREFIX;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getBuilderClassName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getParameterClass;

/**
 * @author Illia Shepilov
 */
//TODO:2017-03-22:illiashepilov: finish implementation.
public class MapMethodConstructor extends AbstractMethodConstructor {

    private final int fieldIndex;
    private final String javaFieldName;
    private final String methodPartName;
    private final ClassName builderClassName;
    private final ClassName genericClassName;
    private final ClassName parameterClassName;
    private final FieldDescriptorProto fieldDescriptor;

    private MapMethodConstructor(MapFieldMethodsConstructorBuilder builder) {
        this.fieldIndex = builder.fieldIndex;
        this.fieldDescriptor = builder.fieldDescriptor;
        this.genericClassName = builder.genericClassName;
        methodPartName = getJavaFieldName(fieldDescriptor.getName(), true);
        javaFieldName = getJavaFieldName(fieldDescriptor.getName(), false);
        builderClassName = getBuilderClassName(builder.javaPackage, builder.javaClass);
        parameterClassName = getParameterClass(fieldDescriptor, builder.messageTypeCache);
    }

    @Override
    Collection<MethodSpec> construct() {
        final List<MethodSpec> methods = newArrayList();
        methods.addAll(createMapMethods());
        methods.addAll(createRawMapMethods());
        return methods;
    }

    private List<MethodSpec> createRawMapMethods() {
        final List<MethodSpec> methods = newArrayList();

        return methods;
    }

    private List<MethodSpec> createMapMethods() {
        final List<MethodSpec> methods = newArrayList();
        methods.add(createPutMethod());

        return methods;
    }

    private MethodSpec createPutMethod() {
        final String methodName = getJavaFieldName(ADD_RAW_PREFIX + methodPartName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, genericClassName);
        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addException(ConstraintViolationThrowable.class)
//                                            .addParameter()
                                            .addStatement(descriptorCodeLine)
                                            .build();
        return result;
    }

    static MapFieldMethodsConstructorBuilder newBuilder() {
        return new MapFieldMethodsConstructorBuilder();
    }

    static class MapFieldMethodsConstructorBuilder {

        private int fieldIndex;
        private String javaClass;
        private String javaPackage;
        private ClassName genericClassName;
        private MessageTypeCache messageTypeCache;
        private FieldDescriptorProto fieldDescriptor;

        MapFieldMethodsConstructorBuilder setFieldIndex(int fieldIndex) {
            checkArgument(fieldIndex >= 0);
            this.fieldIndex = fieldIndex;
            return this;
        }

        MapFieldMethodsConstructorBuilder setJavaPackage(String javaPackage) {
            checkNotNull(javaPackage);
            this.javaPackage = javaPackage;
            return this;
        }

        MapFieldMethodsConstructorBuilder setJavaClass(String javaClass) {
            checkNotNull(javaClass);
            this.javaClass = javaClass;
            return this;
        }

        MapFieldMethodsConstructorBuilder setMessageTypeCache(MessageTypeCache messageTypeCache) {
            checkNotNull(messageTypeCache);
            this.messageTypeCache = messageTypeCache;
            return this;
        }

        MapFieldMethodsConstructorBuilder setFieldDescriptor(FieldDescriptorProto fieldDescriptor) {
            checkNotNull(fieldDescriptor);
            this.fieldDescriptor = fieldDescriptor;
            return this;
        }

        MapFieldMethodsConstructorBuilder setBuilderGenericClass(ClassName genericClassName) {
            this.genericClassName = genericClassName;
            return this;
        }

        MapMethodConstructor build() {
            checkNotNull(javaClass);
            checkNotNull(javaPackage);
            checkNotNull(messageTypeCache);
            checkNotNull(fieldDescriptor);
            checkNotNull(genericClassName);
            checkArgument(fieldIndex >= 0);
            return new MapMethodConstructor(this);
        }
    }
}
