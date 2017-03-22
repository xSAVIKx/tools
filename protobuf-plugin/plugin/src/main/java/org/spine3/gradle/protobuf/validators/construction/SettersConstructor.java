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
import com.google.protobuf.Descriptors;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import org.spine3.base.ConversionException;
import org.spine3.gradle.protobuf.GenerationUtils;
import org.spine3.gradle.protobuf.MessageTypeCache;
import org.spine3.gradle.protobuf.validators.ValidatingUtils;
import org.spine3.validate.ConstraintViolationThrowable;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.SETTER_PREFIX;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getBuilderClassName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getParameterClass;

class SettersConstructor extends AbstractMethodConstructor {

    private final FieldDescriptorProto fieldDescriptor;
    private final int fieldIndex;
    private final ClassName parameterClassName;
    private final ClassName builderClassName;
    private final ClassName builderGenericClassName;
    private final String paramName;
    private final String setterPart;
    private final String fieldName;

    private SettersConstructor(SettersConstructorBuilder builder) {
        this.fieldDescriptor = builder.fieldDescriptor;
        this.fieldIndex = builder.fieldIndex;
        this.builderGenericClassName = builder.genericClassName;
        this.parameterClassName = getParameterClass(fieldDescriptor, builder.messageTypeCache);
        this.builderClassName = getBuilderClassName(builder.javaPackage, builder.javaClass);
        this.paramName = GenerationUtils.getJavaFieldName(fieldDescriptor.getName(), false);
        this.setterPart = GenerationUtils.getJavaFieldName(paramName, true);
        this.fieldName = GenerationUtils.getJavaFieldName(paramName, false);
    }

    @Override
    Collection<MethodSpec> construct() {
        final List<MethodSpec> methods = newArrayList();
        methods.add(constructSetter());

        if (!parameterClassName.equals(ValidatingUtils.getStringClassName())) {
            methods.add(constructRawSetter());
        }

        return methods;
    }

    private MethodSpec constructSetter() {
        final String methodName = SETTER_PREFIX + setterPart;
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, builderGenericClassName);
        final ParameterSpec parameter = createParameterSpec(fieldDescriptor, false);

        final MethodSpec methodSpec = MethodSpec.methodBuilder(methodName)
                                                .addModifiers(Modifier.PUBLIC)
                                                .returns(builderClassName)
                                                .addParameter(parameter)
                                                .addException(ConstraintViolationThrowable.class)
                                                .addStatement(descriptorCodeLine, Descriptors.FieldDescriptor.class)
                                                .addStatement(createValidateStatement(paramName),
                                                              fieldDescriptor.getName())
                                                .addStatement(THIS_POINTER + fieldName + " = " + fieldName)
                                                .addStatement(RETURN_THIS)
                                                .build();
        return methodSpec;
    }

    private MethodSpec constructRawSetter() {
        final String methodName = SETTER_PREFIX + setterPart + "Raw";
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, builderGenericClassName);
        final ParameterSpec parameter = createParameterSpec(fieldDescriptor, true);

        final MethodSpec methodSpec = MethodSpec.methodBuilder(methodName)
                                                .addModifiers(Modifier.PUBLIC)
                                                .returns(builderClassName)
                                                .addParameter(parameter)
                                                .addException(ConstraintViolationThrowable.class)
                                                .addException(ConversionException.class)
                                                .addStatement(descriptorCodeLine, Descriptors.FieldDescriptor.class)
                                                .addStatement("final $T convertedValue = getConvertedValue($T.class, " + paramName + ')',
                                                              parameterClassName,
                                                              parameterClassName)
                                                .addStatement(createValidateConvertedValueStatement(),
                                                              fieldDescriptor.getName())
                                                .addStatement(THIS_POINTER + fieldName + " = convertedValue")
                                                .addStatement(RETURN_THIS)
                                                .build();
        return methodSpec;
    }

    private ParameterSpec createParameterSpec(FieldDescriptorProto fieldDescriptor, boolean raw) {
        final ClassName methodParamClass = raw ? ValidatingUtils.getStringClassName() : parameterClassName;
        final String paramName = GenerationUtils.getJavaFieldName(fieldDescriptor.getName(), false);
        final ParameterSpec result = ParameterSpec.builder(methodParamClass, paramName)
                                                  .build();
        return result;
    }

    static SettersConstructorBuilder newBuilder() {
        return new SettersConstructorBuilder();
    }

    static class SettersConstructorBuilder {

        private String javaPackage;
        private String javaClass;
        private MessageTypeCache messageTypeCache;
        private FieldDescriptorProto fieldDescriptor;
        private ClassName genericClassName;
        private int fieldIndex;

        SettersConstructorBuilder setFieldIndex(int fieldIndex) {
            checkArgument(fieldIndex >= 0);
            this.fieldIndex = fieldIndex;
            return this;
        }

        SettersConstructorBuilder setJavaPackage(String javaPackage) {
            checkNotNull(javaPackage);
            this.javaPackage = javaPackage;
            return this;
        }

        SettersConstructorBuilder setJavaClass(String javaClass) {
            checkNotNull(javaClass);
            this.javaClass = javaClass;
            return this;
        }

        SettersConstructorBuilder setMessageTypeCache(MessageTypeCache messageTypeCache) {
            checkNotNull(messageTypeCache);
            this.messageTypeCache = messageTypeCache;
            return this;
        }

        SettersConstructorBuilder setFieldDescriptor(FieldDescriptorProto fieldDescriptor) {
            checkNotNull(fieldDescriptor);
            this.fieldDescriptor = fieldDescriptor;
            return this;
        }

        SettersConstructorBuilder setGenericClassName(ClassName genericClassName) {
            this.genericClassName = genericClassName;
            return this;
        }

        SettersConstructor build() {
            checkNotNull(javaClass);
            checkNotNull(javaPackage);
            checkNotNull(messageTypeCache);
            checkNotNull(fieldDescriptor);
            checkNotNull(genericClassName);
            checkArgument(fieldIndex >= 0);
            return new SettersConstructor(this);
        }
    }
}
