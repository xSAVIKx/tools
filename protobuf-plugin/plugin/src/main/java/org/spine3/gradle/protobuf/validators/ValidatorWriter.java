package org.spine3.gradle.protobuf.validators;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.gradle.protobuf.GenerationUtils;
import org.spine3.gradle.protobuf.MessageTypeCache;
import org.spine3.server.validate.CommonValidatingBuilder;
import org.spine3.validate.ConstraintViolationThrowable;
import org.spine3.validate.ConversionError;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.gradle.internal.impldep.com.beust.jcommander.internal.Lists.newArrayList;
import static org.spine3.gradle.protobuf.GenerationUtils.getFieldType;
import static org.spine3.gradle.protobuf.GenerationUtils.getJavaFieldName;

/**
 * Class, which writes Validator java code, based on it's descriptor.
 *
 * @author Illia Shepilov
 */
class ValidatorWriter {

    private static final String rootFolder = "generated/main/java/";

    private final String javaClass;
    private final String javaPackage;
    private final DescriptorProto descriptor;
    private final MessageTypeCache messageTypeCache;

    ValidatorWriter(WriterDto writerDto, MessageTypeCache messageTypeCache) {
        this.javaClass = writerDto.getJavaClass();
        this.javaPackage = writerDto.getJavaPackage();
        this.descriptor = writerDto.getMsgDescriptor();
        this.messageTypeCache = messageTypeCache;
    }

    void write() {
        log().debug("Writing the java class under {}", javaPackage);
        final File rootDirectory = new File(rootFolder);
        final List<MethodSpec> methods = newArrayList();

        methods.add(createNewBuilderMethod());
        methods.addAll(createSetters());
        methods.addAll(createRawSetters());
        methods.add(createBuildMethod());
        final MethodSpec constructor = createConstructor();
        methods.add(constructor);

        final Collection<FieldSpec> fields = getFields();

        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(javaClass);
        addFields(classBuilder, fields);
        addMethods(classBuilder, methods);

        final TypeSpec javaClass = classBuilder.build();
        writeClass(rootDirectory, javaClass);
    }

    private Collection<FieldSpec> getFields() {
        final List<FieldSpec> fields = newArrayList();
        for (FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {
            final FieldSpec field = constructField(fieldDescriptor);
            fields.add(field);
        }
        return fields;
    }

    private FieldSpec constructField(FieldDescriptorProto fieldDescriptor) {
        final String fieldName = getJavaFieldName(fieldDescriptor.getName(), false);
        final Class<?> fieldClass = getParameterClass(fieldDescriptor);
        return FieldSpec.builder(fieldClass, fieldName, Modifier.PRIVATE)
                        .build();
    }

    private static TypeSpec.Builder addFields(TypeSpec.Builder classBuilder, Iterable<FieldSpec> fields) {
        return classBuilder.addFields(fields);
    }

    private Collection<MethodSpec> createSetters() {
        final List<MethodSpec> setters = newArrayList();
        int index = 0;
        for (FieldDescriptorProto descriptorField : descriptor.getFieldList()) {
            final Class<?> parameterClass = getParameterClass(descriptorField);
            final String paramName = getJavaFieldName(descriptorField.getName(), false);
            final ParameterSpec parameter = ParameterSpec.builder(parameterClass, paramName)
                                                         .build();
            final String fieldName = getJavaFieldName(paramName, false);
            final String setterPart = getJavaFieldName(paramName, true);
            final String methodName = "set" + setterPart;
            final Class<?> defaultFieldType = getFieldType(descriptorField.getType()
                                                                          .name());
            final String fieldDescriptorType = defaultFieldType != null ? defaultFieldType.getName() : parameterClass.getName();
            final MethodSpec methodSpec = MethodSpec.methodBuilder(methodName)
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .addParameter(parameter)
                                                    .addException(ConstraintViolationThrowable.class)
                                                    .addStatement("final $T fieldDescriptor = " + fieldDescriptorType + ".getDescriptor().getFields().get(" + index + ")", FieldDescriptor.class)
                                                    .addStatement("validate(fieldDescriptor, " + paramName + ", $S)", paramName)
                                                    .addStatement("this." + fieldName + " = " + fieldName)
                                                    .build();
            setters.add(methodSpec);
            ++index;
        }
        return setters;
    }

    private Collection<MethodSpec> createRawSetters() {
        final List<MethodSpec> setters = newArrayList();
        int index = 0;
        for (FieldDescriptorProto descriptorField : descriptor.getFieldList()) {
            final Class<?> parameterClass = getParameterClass(descriptorField);
            if (parameterClass.equals(String.class)) {
                return setters;
            }
            final String paramName = getJavaFieldName(descriptorField.getName(), false);
            final ParameterSpec firstParam = ParameterSpec.builder(String.class, paramName)
                                                          .build();
            final String fieldName = getJavaFieldName(paramName, false);
            final String setterPart = getJavaFieldName(paramName, true);
            final String methodName = "set" + setterPart + "Raw";
            final String descriptorFieldName = descriptorField.getType()
                                                              .name();
            final Class<?> defaultFieldType = getFieldType(descriptorFieldName);
            final String fieldDescriptorType = defaultFieldType != null ? defaultFieldType.getName() : parameterClass.getName();
            final MethodSpec methodSpec = MethodSpec.methodBuilder(methodName)
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .addParameter(firstParam)
                                                    .addException(ConstraintViolationThrowable.class)
                                                    .addException(ConversionError.class)
                                                    .addStatement("final $T fieldDescriptor = " + fieldDescriptorType + ".getDescriptor().getFields().get(" + index + ")", FieldDescriptor.class)
                                                    .addStatement("final $T convertedValue = getConvertedValue($T.class, " + paramName + ")", parameterClass, parameterClass)
                                                    .addStatement("validate(fieldDescriptor, convertedValue, " + paramName + ")")
                                                    .addStatement("this." + fieldName + " = convertedValue")
                                                    .build();
            setters.add(methodSpec);
            ++index;
        }

        return setters;
    }

    private MethodSpec createBuildMethod() {
        final StringBuilder builder = new StringBuilder("final $T result = $T.newBuilder()");
        for (FieldDescriptorProto fieldDescription : descriptor.getFieldList()) {
            builder.append(".")
                   .append("set")
                   .append(getJavaFieldName(fieldDescription.getName(), true))
                   .append("(")
                   .append(getJavaFieldName(fieldDescription.getName(), false))
                   .append(")");
        }
        builder.append(".build()");

        final Class<?> builtMessage = getParameterGeneralClass();
        final MethodSpec buildMethod = MethodSpec.methodBuilder("build")
                                                 .addModifiers(Modifier.PUBLIC)
                                                 .returns(builtMessage)
                                                 .addStatement(builder.toString(), builtMessage, builtMessage)
                                                 .addStatement("return result")
                                                 .build();
        return buildMethod;
    }

    private MethodSpec createNewBuilderMethod() {
        final ClassName builderClass = ClassName.get(javaPackage, javaClass);
        final MethodSpec buildMethod = MethodSpec.methodBuilder("newBuilder")
                                                 .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                 .returns(builderClass)
                                                 .addStatement("return new $T()", builderClass)
                                                 .build();
        return buildMethod;
    }

    private Class<?> getParameterGeneralClass() {
        final Collection<String> values = messageTypeCache.getCachedTypes()
                                                          .values();
        final String expectedClassName = javaPackage + "." + descriptor.getName();
        for (String value : values) {
            if (value.equals(expectedClassName)) {
                try {
                    return Class.forName(value);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new RuntimeException();
    }

    private Class<?> getParameterClass(DescriptorProtos.FieldDescriptorProtoOrBuilder descriptorField) {
        String typeName = descriptorField.getTypeName();
        if (typeName.isEmpty()) {
            return GenerationUtils.getType(descriptorField.getType()
                                                          .name());
        }
        typeName = typeName.substring(1);
        final String parameterType = messageTypeCache.getCachedTypes()
                                                     .get(typeName);
        try {
            return Class.forName(parameterType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeClass(File rootFodler, TypeSpec validator) {
        try {
            JavaFile.builder(javaPackage, validator)
                    .build()
                    .writeTo(rootFodler);
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static MethodSpec createConstructor() {
        final MethodSpec result = MethodSpec.constructorBuilder()
                                            .addModifiers(Modifier.PRIVATE)
                                            .build();
        return result;
    }

    private TypeSpec.Builder addMethods(TypeSpec.Builder builder, Iterable<MethodSpec> methodSpecs) {
        final ParameterizedTypeName superClass =
                ParameterizedTypeName.get(CommonValidatingBuilder.class, getParameterGeneralClass());
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
               .superclass(superClass)
               .addMethods(methodSpecs);
        return builder;
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ValidatorWriter.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
