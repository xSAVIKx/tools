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
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import org.spine3.base.ConversionException;
import org.spine3.gradle.protobuf.GenerationUtils;
import org.spine3.gradle.protobuf.fieldtype.FieldType;
import org.spine3.validate.ConstraintViolationThrowable;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getBuilderClassName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getParameterClass;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getStringClassName;

class SingularFieldMethodConstructor extends AbstractMethodConstructor {

    private final FieldDescriptorProto fieldDescriptor;
    private final int fieldIndex;
    private final ClassName parameterClassName;
    private final ClassName builderClassName;
    private final ClassName builderGenericClassName;
    private final String paramName;
    private final String setterPart;
    private final String fieldName;
    private final FieldType fieldType;

    private SingularFieldMethodConstructor(MethodConstructorBuilder builder) {
        this.fieldType = builder.getFieldType();
        this.fieldDescriptor = builder.getFieldDescriptor();
        this.fieldIndex = builder.getFieldIndex();
        this.builderGenericClassName = builder.getGenericClassName();
        this.parameterClassName = getParameterClass(fieldDescriptor, builder.getMessageTypeCache());
        this.builderClassName = getBuilderClassName(builder.getJavaPackage(), builder.getJavaClass());
        this.paramName = GenerationUtils.getJavaFieldName(fieldDescriptor.getName(), false);
        this.setterPart = GenerationUtils.getJavaFieldName(paramName, true);
        this.fieldName = GenerationUtils.getJavaFieldName(paramName, false);
    }

    @Override
    Collection<MethodSpec> construct() {
        final List<MethodSpec> methods = newArrayList();
        methods.add(constructSetter());

        if (!parameterClassName.equals(getStringClassName())) {
            methods.add(constructRawSetter());
        }

        return methods;
    }

    private MethodSpec constructSetter() {
        final String methodName = fieldType.getSetterPrefix() + setterPart;
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, builderGenericClassName);
        final ParameterSpec parameter = createParameterSpec(fieldDescriptor, false);

        final MethodSpec methodSpec = MethodSpec.methodBuilder(methodName)
                                                .addModifiers(Modifier.PUBLIC)
                                                .returns(builderClassName)
                                                .addParameter(parameter)
                                                .addException(ConstraintViolationThrowable.class)
                                                .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                                .addStatement(createValidateStatement(paramName),
                                                              fieldDescriptor.getName())
                                                .addStatement(THIS_POINTER + fieldName + " = " + fieldName)
                                                .addStatement(RETURN_THIS)
                                                .build();
        return methodSpec;
    }

    private MethodSpec constructRawSetter() {
        final String methodName = fieldType.getSetterPrefix() + setterPart + RAW_SUFFIX;
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, builderGenericClassName);
        final ParameterSpec parameter = createParameterSpec(fieldDescriptor, true);

        final MethodSpec methodSpec = MethodSpec.methodBuilder(methodName)
                                                .addModifiers(Modifier.PUBLIC)
                                                .returns(builderClassName)
                                                .addParameter(parameter)
                                                .addException(ConstraintViolationThrowable.class)
                                                .addException(ConversionException.class)
                                                .addStatement(descriptorCodeLine, FieldDescriptor.class)
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
        final ClassName methodParamClass = raw ? getStringClassName() : parameterClassName;
        final String paramName = GenerationUtils.getJavaFieldName(fieldDescriptor.getName(), false);
        final ParameterSpec result = ParameterSpec.builder(methodParamClass, paramName)
                                                  .build();
        return result;
    }

    static SettersConstructorBuilder newBuilder() {
        return new SettersConstructorBuilder();
    }

    static class SettersConstructorBuilder extends MethodConstructorBuilder {

        @Override
        AbstractMethodConstructor build() {
            checkFields();
            return new SingularFieldMethodConstructor(this);
        }
    }
}