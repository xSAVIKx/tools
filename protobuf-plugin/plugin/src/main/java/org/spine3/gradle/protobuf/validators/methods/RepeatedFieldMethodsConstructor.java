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

package org.spine3.gradle.protobuf.validators.methods;

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
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getBuilderClassName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getParameterClass;

/**
 * @author Illia Shepilov
 */
public class RepeatedFieldMethodsConstructor extends MethodConstructor {

    private static final String CREATE_IF_NEEDED = "createIfNeeded()";
    private static final String ADD_RAW_PREFIX = "addRaw";
    private static final String ADD_PREFIX = "add";
    private static final String REMOVE_PREFIX = "remove";

    private final FieldDescriptorProto fieldDescriptor;
    private final int fieldIndex;
    private final String methodPartName;
    private final ClassName builderClass;
    private final Class<?> parameterClass;
    private final String javaFieldName;
    private final Class<?> builderGenericClass;

    public RepeatedFieldMethodsConstructor(RepeatedFieldMethodsConstructorBuilder builder) {
        this.fieldDescriptor = builder.fieldDescriptor;
        this.fieldIndex = builder.fieldIndex;
        this.builderGenericClass = builder.builderGenericClass;
        methodPartName = getJavaFieldName(fieldDescriptor.getName(), true);
        javaFieldName = getJavaFieldName(fieldDescriptor.getName(), false);
        builderClass = getBuilderClassName(builder.javaPackage, builder.javaClass);
        parameterClass = getParameterClass(fieldDescriptor, builder.messageTypeCache);
    }

    public Collection<MethodSpec> construct() {
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
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, builderGenericClass);

        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClass)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(String.class, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addException(ConversionException.class)
                                            .addStatement(CREATE_IF_NEEDED)
                                            .addStatement(createGetConvertedSingularValue(),
                                                          parameterClass,
                                                          parameterClass)
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
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, builderGenericClass);

        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClass)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(int.class, INDEX)
                                            .addParameter(String.class, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addException(ConversionException.class)
                                            .addStatement(CREATE_IF_NEEDED)
                                            .addStatement(createGetConvertedSingularValue(),
                                                          parameterClass,
                                                          parameterClass)
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
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, builderGenericClass);

        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClass)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(String.class, VALUE)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addException(ConversionException.class)
                                            .addStatement(CREATE_IF_NEEDED)
                                            .addStatement(createGetConvertedPluralValue(),
                                                          List.class,
                                                          parameterClass,
                                                          TypeToken.class,
                                                          List.class,
                                                          parameterClass)
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
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, builderGenericClass);
        final ParameterizedTypeName parameter = ParameterizedTypeName.get(List.class, parameterClass);
        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClass)
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
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, builderGenericClass);

        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClass)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(parameterClass, VALUE)
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
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, builderGenericClass);

        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClass)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addParameter(int.class, INDEX)
                                            .addParameter(parameterClass, VALUE)
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
                                            .returns(builderClass)
                                            .addParameter(parameterClass, VALUE)
                                            .addStatement(javaFieldName + ".remove(value)")
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createRemoveByIndexMethod() {
        final String methodName = getJavaFieldName(REMOVE_PREFIX + methodPartName, false);

        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(builderClass)
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
                                            .returns(builderClass)
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

    public static RepeatedFieldMethodsConstructorBuilder newBuilder() {
        return new RepeatedFieldMethodsConstructorBuilder();
    }

    public static class RepeatedFieldMethodsConstructorBuilder {

        private String javaPackage;
        private String javaClass;
        private MessageTypeCache messageTypeCache;
        private FieldDescriptorProto fieldDescriptor;
        private Class<?> builderGenericClass;
        private int fieldIndex;

        public RepeatedFieldMethodsConstructorBuilder setFieldIndex(int fieldIndex) {
            checkArgument(fieldIndex >= 0);
            this.fieldIndex = fieldIndex;
            return this;
        }

        public RepeatedFieldMethodsConstructorBuilder setJavaPackage(String javaPackage) {
            checkNotNull(javaPackage);
            this.javaPackage = javaPackage;
            return this;
        }

        public RepeatedFieldMethodsConstructorBuilder setJavaClass(String javaClass) {
            checkNotNull(javaClass);
            this.javaClass = javaClass;
            return this;
        }

        public RepeatedFieldMethodsConstructorBuilder setMessageTypeCache(MessageTypeCache messageTypeCache) {
            checkNotNull(messageTypeCache);
            this.messageTypeCache = messageTypeCache;
            return this;
        }

        public RepeatedFieldMethodsConstructorBuilder setFieldDescriptor(FieldDescriptorProto fieldDescriptor) {
            checkNotNull(fieldDescriptor);
            this.fieldDescriptor = fieldDescriptor;
            return this;
        }

        public RepeatedFieldMethodsConstructorBuilder setBuilderGenericClass(Class<?> builderGenericClass) {
            this.builderGenericClass = builderGenericClass;
            return this;
        }

        public RepeatedFieldMethodsConstructor build() {
            checkNotNull(javaClass);
            checkNotNull(javaPackage);
            checkNotNull(messageTypeCache);
            checkNotNull(fieldDescriptor);
            checkNotNull(builderGenericClass);
            checkArgument(fieldIndex >= 0);
            return new RepeatedFieldMethodsConstructor(this);
        }
    }
}
