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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.ConversionException;
import org.spine3.gradle.protobuf.fieldtype.FieldType;
import org.spine3.validate.ConstraintViolationThrowable;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.gradle.protobuf.util.GenerationUtils.getJavaFieldName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getClassName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getParameterClass;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getStringClassName;

/**
 * A method constructor for the singular fields.
 */
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

    private SingularFieldMethodConstructor(AbstractMethodConstructorBuilder builder) {
        this.fieldType = builder.getFieldType();
        this.fieldDescriptor = builder.getFieldDescriptor();
        this.fieldIndex = builder.getFieldIndex();
        this.builderGenericClassName = builder.getGenericClassName();
        this.parameterClassName = getParameterClass(fieldDescriptor, builder.getMessageTypeCache());
        this.builderClassName = getClassName(builder.getJavaPackage(), builder.getJavaClass());
        this.paramName = getJavaFieldName(fieldDescriptor.getName(), false);
        this.setterPart = getJavaFieldName(paramName, true);
        this.fieldName = getJavaFieldName(paramName, false);
    }

    @Override
    Collection<MethodSpec> construct() {
        final String javaFieldName = getJavaFieldName(fieldDescriptor.getName(), false);
        log().debug("The method construction for the {} singular field is started.", javaFieldName);
        final List<MethodSpec> methods = newArrayList();
        methods.add(constructSetter());

        if (!parameterClassName.equals(getStringClassName())) {
            methods.add(constructRawSetter());
        }
        log().debug("The method construction for the {} singular field is finished.", javaFieldName);
        return methods;
    }

    private MethodSpec constructSetter() {
        log().debug("The setters construction for the singular field is started.");
        final String methodName = fieldType.getSetterPrefix() + setterPart;
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex,
                                                                   builderGenericClassName);
        final ParameterSpec parameter = createParameterSpec(fieldDescriptor, false);

        final MethodSpec methodSpec =
                MethodSpec.methodBuilder(methodName)
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
        log().debug("The setters construction for the singular method is finished.");
        return methodSpec;
    }

    private MethodSpec constructRawSetter() {
        log().debug("The raw setters construction is started.");
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
        log().debug("The raw setters construction is finished.");
        return methodSpec;
    }

    private ParameterSpec createParameterSpec(FieldDescriptorProto fieldDescriptor, boolean raw) {
        final ClassName methodParamClass = raw ? getStringClassName() : parameterClassName;
        final String paramName = getJavaFieldName(fieldDescriptor.getName(), false);
        final ParameterSpec result = ParameterSpec.builder(methodParamClass, paramName)
                                                  .build();
        return result;
    }

    /**
     * Creates a new builder for the {@code SingularFieldMethodConstructor} class.
     *
     * @return constructed builder
     */
    static SettersConstructorBuilder newBuilder() {
        return new SettersConstructorBuilder();
    }

    /**
     * A builder class for the {@code SingularFieldMethodConstructor} class.
     */
    static class SettersConstructorBuilder extends AbstractMethodConstructorBuilder {

        @Override
        AbstractMethodConstructor build() {
            checkFields();
            return new SingularFieldMethodConstructor(this);
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(SingularFieldMethodConstructor.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
