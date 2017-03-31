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

import com.google.common.reflect.TypeToken;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.ConversionException;
import org.spine3.gradle.protobuf.failure.fieldtype.FieldType;
import org.spine3.validate.ConstraintViolationThrowable;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.gradle.protobuf.util.GenerationUtils.getJavaFieldName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getClassName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getParameterClassName;

/**
 * A method constructor for the repeated fields based on the ProtoBuf message declaration.
 *
 * @author Illia Shepilov
 */
class RepeatedFieldMethodConstructor extends AbstractMethodConstructor {

    private static final String ADD_PREFIX = "add";
    private static final String ADD_RAW_PREFIX = "addRaw";
    private static final String VALUE = "value";
    private static final String CONVERTED_VALUE = "convertedValue";

    private final int fieldIndex;
    private final FieldType fieldType;
    private final String javaFieldName;
    private final String methodPartName;
    private final ClassName builderClassName;
    private final ClassName listElementClassName;
    private final ClassName builderGenericClassName;
    private final FieldDescriptorProto fieldDescriptor;

    private RepeatedFieldMethodConstructor(AbstractMethodConstructorBuilder builder) {
        this.fieldType = builder.getFieldType();
        this.fieldIndex = builder.getFieldIndex();
        this.fieldDescriptor = builder.getFieldDescriptor();
        this.builderGenericClassName = builder.getGenericClassName();
        this.javaFieldName = getJavaFieldName(fieldDescriptor.getName(), false);
        this.methodPartName = getJavaFieldName(fieldDescriptor.getName(), true);
        this.builderClassName = getClassName(builder.getJavaPackage(), builder.getJavaClass());
        this.listElementClassName = getParameterClassName(fieldDescriptor,
                                                          builder.getMessageTypeCache());
    }

    @Override
    Collection<MethodSpec> construct() {
        log().debug("The methods construction for the {} repeated field is started.",
                    javaFieldName);
        final List<MethodSpec> methods = newArrayList();
        methods.addAll(createRepeatedMethods());
        methods.addAll(createRepeatedRawMethods());
        log().debug("The methods construction for the {} repeated field is finished.",
                    javaFieldName);
        return methods;
    }

    private Collection<MethodSpec> createRepeatedRawMethods() {
        log().debug("The raw methods construction for the repeated field is is started.");
        final List<MethodSpec> methods = newArrayList();

        methods.add(createRawAddByIndexMethod());
        methods.add(createRawAddObjectMethod());
        methods.add(createRawAddAllMethod());

        log().debug("The raw methods construction for the repeated field is is finished.");
        return methods;
    }

    private Collection<MethodSpec> createRepeatedMethods() {
        final List<MethodSpec> methods = newArrayList();

        methods.add(createClearMethod());
        methods.add(createAddByIndexMethod());
        methods.add(createAddObjectMethod());
        methods.add(createRemoveByIndexMethod());
        methods.add(createRemoveObject());
        methods.add(createAddAllMethod());

        return methods;
    }

    private MethodSpec createRawAddObjectMethod() {
        final String methodName = getJavaFieldName(ADD_RAW_PREFIX + methodPartName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex,
                                                                   builderGenericClassName);
        final String addValueStatement = THIS_POINTER + javaFieldName + ".add(convertedValue)";
        final String convertStatement = createValidateConvertedValueStatement(CONVERTED_VALUE);
        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(String.class, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addException(ConversionException.class)
                                            .addStatement(CALL_INITIALIZE_IF_NEEDED)
                                            .addStatement(createGetConvertedSingularValue(VALUE),
                                                          listElementClassName,
                                                          listElementClassName)
                                            .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                            .addStatement(convertStatement,
                                                          fieldDescriptor.getName())
                                            .addStatement(addValueStatement)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createRawAddByIndexMethod() {
        final String methodName = getJavaFieldName(ADD_RAW_PREFIX + methodPartName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex,
                                                                   builderGenericClassName);
        final String addByIndex = THIS_POINTER + javaFieldName + ".add(index, convertedValue)";
        final MethodSpec result =
                MethodSpec.methodBuilder(methodName)
                          .returns(builderClassName)
                          .addModifiers(Modifier.PUBLIC)
                          .addParameter(int.class, INDEX)
                          .addParameter(String.class, VALUE)
                          .addException(ConstraintViolationThrowable.class)
                          .addException(ConversionException.class)
                          .addStatement(CALL_INITIALIZE_IF_NEEDED)
                          .addStatement(createGetConvertedSingularValue(VALUE),
                                        listElementClassName,
                                        listElementClassName)
                          .addStatement(descriptorCodeLine, FieldDescriptor.class)
                          .addStatement(createValidateConvertedValueStatement(CONVERTED_VALUE),
                                        fieldDescriptor.getName())
                          .addStatement(addByIndex)
                          .addStatement(RETURN_THIS)
                          .build();
        return result;
    }

    private MethodSpec createRawAddAllMethod() {
        final String rawMethodName = fieldType.getSetterPrefix() + RAW_SUFFIX + methodPartName;
        final String methodName = getJavaFieldName(rawMethodName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex,
                                                                   builderGenericClassName);
        final String addAllValues = THIS_POINTER + javaFieldName + ADD_ALL_CONVERTED_VALUE;
        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(String.class, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addException(ConversionException.class)
                                            .addStatement(CALL_INITIALIZE_IF_NEEDED)
                                            .addStatement(createGetConvertedCollectionValue(),
                                                          List.class,
                                                          listElementClassName,
                                                          TypeToken.class,
                                                          List.class,
                                                          listElementClassName)
                                            .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                            .addStatement(createValidateConvertedValueStatement(CONVERTED_VALUE),
                                                          fieldDescriptor.getName())
                                            .addStatement(addAllValues)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createAddAllMethod() {
        final String rawMethodName = fieldType.getSetterPrefix() + methodPartName;
        final String methodName = getJavaFieldName(rawMethodName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex,
                                                                   builderGenericClassName);
        final ClassName rawType = ClassName.get(List.class);
        final ParameterizedTypeName parameter = ParameterizedTypeName.get(rawType,
                                                                          listElementClassName);
        final String fieldName = fieldDescriptor.getName();
        final String addAllValues = THIS_POINTER + javaFieldName + ".addAll(value)";
        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(parameter, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addException(ConversionException.class)
                                            .addStatement(CALL_INITIALIZE_IF_NEEDED)
                                            .addStatement(descriptorCodeLine,
                                                          FieldDescriptor.class)
                                            .addStatement(createValidateStatement(fieldName),
                                                          fieldName)
                                            .addStatement(addAllValues)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createAddObjectMethod() {
        final String rawMethodName = ADD_PREFIX + methodPartName;
        final String methodName = getJavaFieldName(rawMethodName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex,
                                                                   builderGenericClassName);
        final String addValue = THIS_POINTER + javaFieldName + ".add(value)";
        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(listElementClassName, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addStatement(CALL_INITIALIZE_IF_NEEDED)
                                            .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                            .addStatement(createValidateStatement(javaFieldName),
                                                          javaFieldName)
                                            .addStatement(addValue)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createAddByIndexMethod() {
        final String methodName = getJavaFieldName(ADD_PREFIX + methodPartName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex,
                                                                   builderGenericClassName);
        final String addValueByIndex = THIS_POINTER + javaFieldName + ".add(index, value)";
        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(int.class, INDEX)
                                            .addParameter(listElementClassName, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addStatement(addValueByIndex)
                                            .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                            .addStatement(createValidateStatement(javaFieldName),
                                                          javaFieldName)
                                            .addStatement(CALL_INITIALIZE_IF_NEEDED)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createRemoveObject() {
        final String removeMethodName = getJavaFieldName(REMOVE_PREFIX + methodPartName, false);
        final String removeValue = THIS_POINTER + javaFieldName + ".remove(value)";
        final MethodSpec result = MethodSpec.methodBuilder(removeMethodName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(builderClassName)
                                            .addParameter(listElementClassName, VALUE)
                                            .addStatement(CALL_INITIALIZE_IF_NEEDED)
                                            .addStatement(removeValue)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createRemoveByIndexMethod() {
        final String methodName = getJavaFieldName(REMOVE_PREFIX + methodPartName, false);
        final String removeByIndex = THIS_POINTER + javaFieldName + ".remove(index)";
        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(builderClassName)
                                            .addParameter(int.class, INDEX)
                                            .addStatement(CALL_INITIALIZE_IF_NEEDED)
                                            .addStatement(removeByIndex)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createClearMethod() {
        final String clearField = THIS_POINTER + javaFieldName + CLEAR_METHOD_CALL;
        final MethodSpec result = MethodSpec.methodBuilder(CLEAR_PREFIX)
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(builderClassName)
                                            .addStatement(CALL_INITIALIZE_IF_NEEDED)
                                            .addStatement(clearField)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private static String createGetConvertedCollectionValue() {
        final String result = "final $T<$T> convertedValue = " +
                "getConvertedValue(new $T<$T<$T>>(){}.getType(), value)";
        return result;
    }

    /**
     * Creates a new builder for the {@code RepeatedFieldMethodConstructor} class.
     *
     * @return created builder
     */
    static RepeatedFieldMethodsConstructorBuilder newBuilder() {
        return new RepeatedFieldMethodsConstructorBuilder();
    }

    /**
     * A builder for the {@code RepeatedFieldMethodConstructor} class.
     */
    static class RepeatedFieldMethodsConstructorBuilder extends AbstractMethodConstructorBuilder {

        @Override
        RepeatedFieldMethodConstructor build() {
            checkFields();
            return new RepeatedFieldMethodConstructor(this);
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(RepeatedFieldMethodConstructor.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
