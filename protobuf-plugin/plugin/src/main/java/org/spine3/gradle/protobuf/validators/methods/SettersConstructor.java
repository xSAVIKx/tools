package org.spine3.gradle.protobuf.validators.methods;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.Descriptors;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import org.gradle.internal.impldep.com.beust.jcommander.internal.Lists;
import org.spine3.base.SingularKey;
import org.spine3.gradle.protobuf.GenerationUtils;
import org.spine3.gradle.protobuf.MessageTypeCache;
import org.spine3.validate.ConstraintViolationThrowable;
import org.spine3.validate.ConversionError;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.List;

import static org.gradle.internal.impldep.com.google.common.base.Preconditions.checkArgument;
import static org.gradle.internal.impldep.com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.SETTER_PREFIX;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getBuilderClassName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getParameterClass;

public class SettersConstructor extends MethodConstructor {

    private final FieldDescriptorProto fieldDescriptor;
    private final int fieldIndex;
    private final Class<?> parameterClass;
    private final ClassName builderClassName;
    private final Class<?> builderGenericClass;
    private final String paramName;
    private final String setterPart;
    private final String fieldName;

    private SettersConstructor(SettersConstructorBuilder builder) {
        this.fieldDescriptor = builder.fieldDescriptor;
        this.fieldIndex = builder.fieldIndex;
        this.builderGenericClass = builder.builderGenericClass;
        this.parameterClass = getParameterClass(fieldDescriptor, builder.messageTypeCache);
        this.builderClassName = getBuilderClassName(builder.javaPackage, builder.javaClass);
        this.paramName = GenerationUtils.getJavaFieldName(fieldDescriptor.getName(), false);
        this.setterPart = GenerationUtils.getJavaFieldName(paramName, true);
        this.fieldName = GenerationUtils.getJavaFieldName(paramName, false);
    }

    public Collection<MethodSpec> construct() {
        final List<MethodSpec> methods = Lists.newArrayList();
        methods.add(constructSetter());

        if (!parameterClass.equals(String.class)) {
            methods.add(constructRawSetter());
        }

        return methods;
    }

    private MethodSpec constructSetter() {
        final String methodName = SETTER_PREFIX + setterPart;
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, builderGenericClass);
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
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, builderGenericClass);
        final ParameterSpec parameter = createParameterSpec(fieldDescriptor, true);

        final MethodSpec methodSpec = MethodSpec.methodBuilder(methodName)
                                                .addModifiers(Modifier.PUBLIC)
                                                .returns(builderClassName)
                                                .addParameter(parameter)
                                                .addException(ConstraintViolationThrowable.class)
                                                .addException(ConversionError.class)
                                                .addStatement(descriptorCodeLine, Descriptors.FieldDescriptor.class)
                                                .addStatement("final $T convertedValue = getConvertedValue(new $T<>($T.class), " + paramName + ')',
                                                              parameterClass,
                                                              SingularKey.class,
                                                              parameterClass)
                                                .addStatement(createValidateConvertedValueStatement(),
                                                              fieldDescriptor.getName())
                                                .addStatement(THIS_POINTER + fieldName + " = convertedValue")
                                                .addStatement(RETURN_THIS)
                                                .build();
        return methodSpec;
    }

    private ParameterSpec createParameterSpec(FieldDescriptorProto fieldDescriptor, boolean raw) {
        final Class<?> methodParamClass = raw ? String.class : parameterClass;
        final String paramName = GenerationUtils.getJavaFieldName(fieldDescriptor.getName(), false);
        final ParameterSpec result = ParameterSpec.builder(methodParamClass, paramName)
                                                  .build();
        return result;
    }

    public static SettersConstructorBuilder newBuilder() {
        return new SettersConstructorBuilder();
    }

    public static class SettersConstructorBuilder {

        private String javaPackage;
        private String javaClass;
        private MessageTypeCache messageTypeCache;
        private FieldDescriptorProto fieldDescriptor;
        private Class<?> builderGenericClass;
        private int fieldIndex;

        public SettersConstructorBuilder setFieldIndex(int fieldIndex) {
            checkArgument(fieldIndex >= 0);
            this.fieldIndex = fieldIndex;
            return this;
        }

        public SettersConstructorBuilder setJavaPackage(String javaPackage) {
            checkNotNull(javaPackage);
            this.javaPackage = javaPackage;
            return this;
        }

        public SettersConstructorBuilder setJavaClass(String javaClass) {
            checkNotNull(javaClass);
            this.javaClass = javaClass;
            return this;
        }

        public SettersConstructorBuilder setMessageTypeCache(MessageTypeCache messageTypeCache) {
            checkNotNull(messageTypeCache);
            this.messageTypeCache = messageTypeCache;
            return this;
        }

        public SettersConstructorBuilder setFieldDescriptor(FieldDescriptorProto fieldDescriptor) {
            checkNotNull(fieldDescriptor);
            this.fieldDescriptor = fieldDescriptor;
            return this;
        }

        public SettersConstructorBuilder setBuilderGenericClass(Class<?> builderGenericClass) {
            this.builderGenericClass = builderGenericClass;
            return this;
        }

        public SettersConstructor build() {
            checkNotNull(javaClass);
            checkNotNull(javaPackage);
            checkNotNull(messageTypeCache);
            checkNotNull(fieldDescriptor);
            checkNotNull(builderGenericClass);
            checkArgument(fieldIndex >= 0);
            return new SettersConstructor(this);
        }
    }
}
