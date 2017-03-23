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
import com.google.protobuf.Descriptors;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import org.spine3.base.ConversionException;
import org.spine3.gradle.protobuf.MessageTypeCache;
import org.spine3.validate.ConstraintViolationThrowable;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.gradle.protobuf.GenerationUtils.getJavaFieldName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.ADD_ALL_PREFIX;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.ADD_RAW_PREFIX;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.CREATE_IF_NEEDED;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getBuilderClassName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getParameterClass;

/**
 * @author Illia Shepilov
 */
class RepeatedFieldMethodsConstructor extends AbstractMethodConstructor {

    private static final String ADD_PREFIX = "add";
    private static final String REMOVE_PREFIX = "remove";

    private final int fieldIndex;
    private final String javaFieldName;
    private final String methodPartName;
    private final ClassName builderClassName;
    private final ClassName genericClassName;
    private final ClassName parameterClassName;
    private final FieldDescriptorProto fieldDescriptor;

    private RepeatedFieldMethodsConstructor(RepeatedFieldMethodsConstructorBuilder builder) {
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
        methods.addAll(createRepeatedMethods());
        methods.addAll(createRepeatedRawMethods());
        return methods;
    }

    private Collection<MethodSpec> createRepeatedRawMethods() {
        final List<MethodSpec> methods = newArrayList();

        methods.add(createRawAddByIndexMethod());
        methods.add(createRawAddObjectMethod());
        methods.add(createRawAddAllMethod());

        return methods;
    }

    private Collection<MethodSpec> createRepeatedMethods() {
        final List<MethodSpec> methods = newArrayList();
        final MethodSpec checkRepeatedFieldMethod = createCheckRepeatedFieldMethod();

        methods.add(checkRepeatedFieldMethod);
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
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, genericClassName);

        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(String.class, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addException(ConversionException.class)
                                            .addStatement(CREATE_IF_NEEDED)
                                            .addStatement(createGetConvertedSingularValue(),
                                                          parameterClassName,
                                                          parameterClassName)
                                            .addStatement(descriptorCodeLine, Descriptors.FieldDescriptor.class)
                                            .addStatement(createValidateConvertedValueStatement(),
                                                          fieldDescriptor.getName())
                                            .addStatement(javaFieldName + ".add(convertedValue)")
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createRawAddByIndexMethod() {
        final String methodName = getJavaFieldName(ADD_RAW_PREFIX + methodPartName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, genericClassName);

        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(int.class, INDEX)
                                            .addParameter(String.class, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addException(ConversionException.class)
                                            .addStatement(CREATE_IF_NEEDED)
                                            .addStatement(createGetConvertedSingularValue(),
                                                          parameterClassName,
                                                          parameterClassName)
                                            .addStatement(descriptorCodeLine, Descriptors.FieldDescriptor.class)
                                            .addStatement(createValidateConvertedValueStatement(),
                                                          fieldDescriptor.getName())
                                            .addStatement(javaFieldName + ".add(index, convertedValue)")
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createRawAddAllMethod() {
        final String methodName = getJavaFieldName("addAllRaw" + methodPartName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, genericClassName);

        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(String.class, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addException(ConversionException.class)
                                            .addStatement(CREATE_IF_NEEDED)
                                            .addStatement(createGetConvertedPluralValue(),
                                                          List.class,
                                                          parameterClassName,
                                                          TypeToken.class,
                                                          List.class,
                                                          parameterClassName)
                                            .addStatement(descriptorCodeLine, Descriptors.FieldDescriptor.class)
                                            .addStatement(createValidateConvertedValueStatement(),
                                                          fieldDescriptor.getName())
                                            .addStatement(javaFieldName + ADD_ALL_CONVERTED_VALUE)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createAddAllMethod() {
        final String methodName = getJavaFieldName(ADD_ALL_PREFIX + methodPartName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, genericClassName);
        final ClassName rawType = ClassName.get(List.class);
        final ParameterizedTypeName parameter = ParameterizedTypeName.get(rawType, parameterClassName);
        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(parameter, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addException(ConversionException.class)
                                            .addStatement(CREATE_IF_NEEDED)
                                            .addStatement(descriptorCodeLine, Descriptors.FieldDescriptor.class)
                                            .addStatement(createValidateStatement(fieldDescriptor.getName()),
                                                          fieldDescriptor.getName())
                                            .addStatement(javaFieldName + ".addAll(value)")
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createAddObjectMethod() {
        final String methodName = getJavaFieldName(ADD_PREFIX + methodPartName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, genericClassName);

        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(parameterClassName, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addStatement(CREATE_IF_NEEDED)
                                            .addStatement(descriptorCodeLine, Descriptors.FieldDescriptor.class)
                                            .addStatement(createValidateStatement(javaFieldName), javaFieldName)
                                            .addStatement(javaFieldName + ".add(value)")
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createAddByIndexMethod() {
        final String methodName = getJavaFieldName(ADD_PREFIX + methodPartName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, genericClassName);

        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(int.class, INDEX)
                                            .addParameter(parameterClassName, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addStatement(javaFieldName + ".add(index, value)")
                                            .addStatement(descriptorCodeLine, Descriptors.FieldDescriptor.class)
                                            .addStatement(createValidateStatement(javaFieldName), javaFieldName)
                                            .addStatement(CREATE_IF_NEEDED)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createRemoveObject() {
        final String removeMethodName = getJavaFieldName(REMOVE_PREFIX + methodPartName, false);
        final MethodSpec result = MethodSpec.methodBuilder(removeMethodName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(builderClassName)
                                            .addParameter(parameterClassName, VALUE)
                                            .addStatement(CREATE_IF_NEEDED)
                                            .addStatement(javaFieldName + ".remove(value)")
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createRemoveByIndexMethod() {
        final String methodName = getJavaFieldName(REMOVE_PREFIX + methodPartName, false);

        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(builderClassName)
                                            .addParameter(int.class, INDEX)
                                            .addStatement(CREATE_IF_NEEDED)
                                            .addStatement(javaFieldName + ".remove(index)")
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createClearMethod() {
        final MethodSpec result = MethodSpec.methodBuilder("clear")
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(builderClassName)
                                            .addStatement(CREATE_IF_NEEDED)
                                            .addStatement(javaFieldName + ".clear()")
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createCheckRepeatedFieldMethod() {
        final MethodSpec result = MethodSpec.methodBuilder("createIfNeeded")
                                            .addModifiers(Modifier.PRIVATE)
                                            .beginControlFlow("if(" + javaFieldName + " == null)")
                                            .addStatement(javaFieldName + " = new $T<>()", ArrayList.class)
                                            .endControlFlow()
                                            .build();
        return result;
    }

    static RepeatedFieldMethodsConstructorBuilder newBuilder() {
        return new RepeatedFieldMethodsConstructorBuilder();
    }

    static class RepeatedFieldMethodsConstructorBuilder {

        private int fieldIndex;
        private String javaClass;
        private String javaPackage;
        private ClassName genericClassName;
        private MessageTypeCache messageTypeCache;
        private FieldDescriptorProto fieldDescriptor;

        RepeatedFieldMethodsConstructorBuilder setFieldIndex(int fieldIndex) {
            checkArgument(fieldIndex >= 0);
            this.fieldIndex = fieldIndex;
            return this;
        }

        RepeatedFieldMethodsConstructorBuilder setJavaPackage(String javaPackage) {
            checkNotNull(javaPackage);
            this.javaPackage = javaPackage;
            return this;
        }

        RepeatedFieldMethodsConstructorBuilder setJavaClass(String javaClass) {
            checkNotNull(javaClass);
            this.javaClass = javaClass;
            return this;
        }

        RepeatedFieldMethodsConstructorBuilder setMessageTypeCache(MessageTypeCache messageTypeCache) {
            checkNotNull(messageTypeCache);
            this.messageTypeCache = messageTypeCache;
            return this;
        }

        RepeatedFieldMethodsConstructorBuilder setFieldDescriptor(FieldDescriptorProto fieldDescriptor) {
            checkNotNull(fieldDescriptor);
            this.fieldDescriptor = fieldDescriptor;
            return this;
        }

        RepeatedFieldMethodsConstructorBuilder setBuilderGenericClass(ClassName genericClassName) {
            this.genericClassName = genericClassName;
            return this;
        }

        RepeatedFieldMethodsConstructor build() {
            checkNotNull(javaClass);
            checkNotNull(javaPackage);
            checkNotNull(messageTypeCache);
            checkNotNull(fieldDescriptor);
            checkNotNull(genericClassName);
            checkArgument(fieldIndex >= 0);
            return new RepeatedFieldMethodsConstructor(this);
        }
    }
}
