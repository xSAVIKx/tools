package org.spine3.gradle.protobuf.validators;

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
import java.util.ArrayList;
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

    private static final String ROOT_FOLDER = "generated/main/java/";
    private static final String REPEATED_FIELD_LABEL = "LABEL_REPEATED";
    private static final String CREATE_IF_NEEDED = "createIfNeeded()";
    public static final String SETTER_PREFIX = "set";

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
        final File rootDirectory = new File(ROOT_FOLDER);
        final List<MethodSpec> methods = newArrayList();

        final MethodSpec constructor = createConstructor();
        methods.add(constructor);
        methods.add(createNewBuilderMethod());
        methods.addAll(createSetters());
        methods.addAll(createRawSetters());
        methods.add(createBuildMethod());
        methods.addAll(createRepeatedFieldMethods());

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
            final String labelName = fieldDescriptor.getLabel()
                                                    .name();
            if (labelName.equals(REPEATED_FIELD_LABEL)) {
                final FieldSpec repeatedField = constructRepeatedField(fieldDescriptor);
                fields.add(repeatedField);
                continue;
            }
            final FieldSpec field = constructField(fieldDescriptor);
            fields.add(field);
        }
        return fields;
    }

    private FieldSpec constructRepeatedField(FieldDescriptorProto fieldDescriptor) {
        final ParameterizedTypeName param = ParameterizedTypeName.get(List.class, getParameterClass(fieldDescriptor));
        final String fieldName = getJavaFieldName(fieldDescriptor.getName(), false);
        return FieldSpec.builder(param, fieldName, Modifier.PRIVATE)
                        .build();
    }

    private FieldSpec constructField(FieldDescriptorProto fieldDescriptor) {
        final String fieldName = getJavaFieldName(fieldDescriptor.getName(), false);
        final Class<?> fieldClass = getParameterClass(fieldDescriptor);
        final FieldSpec result = FieldSpec.builder(fieldClass, fieldName, Modifier.PRIVATE)
                                          .build();
        return result;
    }

    private static TypeSpec.Builder addFields(TypeSpec.Builder classBuilder, Iterable<FieldSpec> fields) {
        return classBuilder.addFields(fields);
    }

    private Collection<MethodSpec> createSetters() {
        final List<MethodSpec> setters = newArrayList();
        int index = 0;
        for (FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {

            final String fieldLabel = fieldDescriptor.getLabel()
                                                     .name();
            if (fieldLabel.equals(REPEATED_FIELD_LABEL)) {
                continue;
            }

            final String paramName = getJavaFieldName(fieldDescriptor.getName(), false);
            final ParameterSpec parameter = createParameterSpec(fieldDescriptor, false);

            final String fieldName = getJavaFieldName(paramName, false);
            final String setterPart = getJavaFieldName(paramName, true);
            final String methodName = SETTER_PREFIX + setterPart;
            final ClassName builderClassName = getBuilderClassName();

            final String descriptorCodeLine = createDescriptorCodeLine(index, fieldDescriptor);
            final MethodSpec methodSpec = MethodSpec.methodBuilder(methodName)
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .returns(builderClassName)
                                                    .addParameter(parameter)
                                                    .addException(ConstraintViolationThrowable.class)
                                                    .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                                    .addStatement("validate(fieldDescriptor, " + paramName + ", $S)", paramName)
                                                    .addStatement("this." + fieldName + " = " + fieldName)
                                                    .addStatement("return this")
                                                    .build();
            setters.add(methodSpec);
            ++index;
        }
        return setters;
    }

    private Collection<MethodSpec> createRawSetters() {
        final List<MethodSpec> setters = newArrayList();
        int index = 0;
        for (FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {
            final Class<?> parameterClass = getParameterClass(fieldDescriptor);

            // To avoid useless conversion from String to String.
            if (parameterClass.equals(String.class)) {
                return setters;
            }

            final String fieldLabel = fieldDescriptor.getLabel()
                                                     .name();

            if (fieldLabel.equals(REPEATED_FIELD_LABEL)) {
                continue;
            }

            final String paramName = getJavaFieldName(fieldDescriptor.getName(), false);
            final ParameterSpec parameter = createParameterSpec(fieldDescriptor, true);

            final String fieldName = getJavaFieldName(paramName, false);
            final String setterPart = getJavaFieldName(paramName, true);
            final String methodName = SETTER_PREFIX + setterPart + "Raw";
            final ClassName builderClassName = getBuilderClassName();

            final String descriptorCodeLine = createDescriptorCodeLine(index, fieldDescriptor);
            final MethodSpec methodSpec = MethodSpec.methodBuilder(methodName)
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .returns(builderClassName)
                                                    .addParameter(parameter)
                                                    .addException(ConstraintViolationThrowable.class)
                                                    .addException(ConversionError.class)
                                                    .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                                    .addStatement("final $T convertedValue = getConvertedValue($T.class, " + paramName + ")", parameterClass, parameterClass)
                                                    .addStatement("validate(fieldDescriptor, convertedValue, " + paramName + ")")
                                                    .addStatement("this." + fieldName + " = convertedValue")
                                                    .addStatement("return this")
                                                    .build();
            setters.add(methodSpec);
            ++index;
        }

        return setters;
    }

    private ParameterSpec createParameterSpec(FieldDescriptorProto fieldDescriptor, boolean raw) {
        final Class<?> parameterClass = raw ? String.class : getParameterClass(fieldDescriptor);
        final String paramName = getJavaFieldName(fieldDescriptor.getName(), false);
        final ParameterSpec result = ParameterSpec.builder(parameterClass, paramName)
                                                  .build();
        return result;
    }

    private String getFieldDescriptorType(FieldDescriptorProto fieldDescriptor) {
        final Class<?> parameterClass = getParameterClass(fieldDescriptor);
        final String descriptorTypeName = fieldDescriptor.getType()
                                                         .name();
        final Class<?> defaultFieldType = getFieldType(descriptorTypeName);
        final String result = defaultFieldType != null ? defaultFieldType.getName() : parameterClass.getName();
        return result;
    }

    private String createDescriptorCodeLine(int index, FieldDescriptorProto fieldDescriptor) {
        final String fieldDescriptorType = getFieldDescriptorType(fieldDescriptor);
        final String result = "final $T fieldDescriptor = " + fieldDescriptorType +
                ".getDescriptor().getFields().get(" + index + ")";
        return result;
    }

    private ClassName getBuilderClassName() {
        final ClassName builderClassName = ClassName.get(javaPackage, javaClass);
        return builderClassName;
    }

    private class RepeatedFieldMethodsConstructor {

        private final FieldDescriptorProto fieldDescriptor;
        private final int fieldIndex;
        private final String methodPartName;
        private final ClassName builderClass;
        private final Class<?> parameterClass;
        private final String javaFieldName;

        public RepeatedFieldMethodsConstructor(FieldDescriptorProto fieldDescriptor, int fieldIndex) {
            this.fieldDescriptor = fieldDescriptor;
            this.fieldIndex = fieldIndex;
            methodPartName = getJavaFieldName(fieldDescriptor.getName(), true);
            javaFieldName = getJavaFieldName(fieldDescriptor.getName(), false);
            builderClass = getBuilderClassName();
            parameterClass = getParameterClass(fieldDescriptor);
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

            return methods;
        }

        private MethodSpec createRawAddObjectMethod() {
            final String methodName = getJavaFieldName("addRaw" + methodPartName, false);
            final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, fieldDescriptor);

            final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                                .returns(builderClass)
                                                .addModifiers(Modifier.PUBLIC)
                                                .addParameter(String.class, "value")
                                                .addException(ConstraintViolationThrowable.class)
                                                .addException(ConversionError.class)
                                                .addStatement(CREATE_IF_NEEDED)
                                                .addStatement("final $T convertedValue = getConvertedValue($T.class, value)", parameterClass, parameterClass)
                                                .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                                .addStatement("validate(fieldDescriptor, convertedValue, $S)", fieldDescriptor.getName())
                                                .addStatement(javaFieldName + ".add(convertedValue)")
                                                .addStatement("return this")
                                                .build();
            return result;
        }

        private MethodSpec createRawAddByIndexMethod() {
            final String methodName = getJavaFieldName("addRaw" + methodPartName, false);
            final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, fieldDescriptor);

            final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                                .returns(builderClass)
                                                .addModifiers(Modifier.PUBLIC)
                                                .addParameter(int.class, "index")
                                                .addParameter(String.class, "value")
                                                .addException(ConstraintViolationThrowable.class)
                                                .addException(ConversionError.class)
                                                .addStatement(CREATE_IF_NEEDED)
                                                .addStatement("final $T convertedValue = getConvertedValue($T.class, value)", parameterClass, parameterClass)
                                                .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                                .addStatement("validate(fieldDescriptor, convertedValue, $S)", fieldDescriptor.getName())
                                                .addStatement(javaFieldName + ".add(index, convertedValue)")
                                                .addStatement("return this")
                                                .build();
            return result;
        }

        private MethodSpec createAddObjectMethod() {
            final String methodName = getJavaFieldName("add" + methodPartName, false);
            final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, fieldDescriptor);

            final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                                .returns(builderClass)
                                                .addModifiers(Modifier.PUBLIC)
                                                .addParameter(parameterClass, "value")
                                                .addException(ConstraintViolationThrowable.class)
                                                .addStatement(CREATE_IF_NEEDED)
                                                .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                                .addStatement("validate(fieldDescriptor, " + javaFieldName + ", $S)", javaFieldName)
                                                .addStatement(javaFieldName + ".add(value)")
                                                .addStatement("return this")
                                                .build();
            return result;
        }

        private MethodSpec createAddByIndexMethod() {
            final String methodName = getJavaFieldName("add" + methodPartName, false);
            final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, fieldDescriptor);

            final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                                .returns(builderClass)
                                                .addModifiers(Modifier.PUBLIC)
                                                .addParameter(int.class, "index")
                                                .addParameter(parameterClass, "value")
                                                .addException(ConstraintViolationThrowable.class)
                                                .addStatement(javaFieldName + ".add(index, value)")
                                                .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                                .addStatement("validate(fieldDescriptor, " + javaFieldName + ", $S)", javaFieldName)
                                                .addStatement(CREATE_IF_NEEDED)
                                                .addStatement("return this")
                                                .build();
            return result;
        }

        private MethodSpec createRemoveObject() {
            final String removeMethodName = getJavaFieldName("remove" + methodPartName, false);
            final MethodSpec result = MethodSpec.methodBuilder(removeMethodName)
                                                .addModifiers(Modifier.PUBLIC)
                                                .returns(builderClass)
                                                .addParameter(parameterClass, "value")
                                                .addStatement(javaFieldName + ".remove(value)")
                                                .addStatement("return this")
                                                .build();
            return result;
        }

        private MethodSpec createRemoveByIndexMethod() {
            final String methodName = getJavaFieldName("remove" + methodPartName, false);

            final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                                .addModifiers(Modifier.PUBLIC)
                                                .returns(builderClass)
                                                .addParameter(int.class, "index")
                                                .addStatement(CREATE_IF_NEEDED)
                                                .addStatement(javaFieldName + ".remove(index)")
                                                .addStatement("return this")
                                                .build();
            return result;
        }

        private MethodSpec createClearMethod() {
            final MethodSpec result = MethodSpec.methodBuilder("clear")
                                                .addModifiers(Modifier.PUBLIC)
                                                .returns(builderClass)
                                                .addStatement(CREATE_IF_NEEDED)
                                                .addStatement(javaFieldName + ".clear()")
                                                .addStatement("return this")
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
    }

    private Collection<MethodSpec> createRepeatedFieldMethods() {
        final List<MethodSpec> methods = newArrayList();
        int fieldIndex = 0;
        for (FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {
            final String labelName = fieldDescriptor.getLabel()
                                                    .name();
            if (labelName.equals(REPEATED_FIELD_LABEL)) {
                final Collection<MethodSpec> repeatedFieldMethods =
                        new RepeatedFieldMethodsConstructor(fieldDescriptor, fieldIndex).construct();
                methods.addAll(repeatedFieldMethods);
            }
            ++fieldIndex;
        }
        return methods;
    }

    private MethodSpec createBuildMethod() {
        final StringBuilder builder = new StringBuilder("final $T result = $T.newBuilder()");
        for (FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {
            builder.append(".");

            if (isRepeatedField(fieldDescriptor)) {
                builder.append("addAll");
            } else {
                builder.append(SETTER_PREFIX);
            }

            builder.append(getJavaFieldName(fieldDescriptor.getName(), true))
                   .append("(")
                   .append(getJavaFieldName(fieldDescriptor.getName(), false))
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

    private static boolean isRepeatedField(FieldDescriptorProto fieldDescriptor) {
        final String labelName = fieldDescriptor.getLabel()
                                                .name();
        final boolean result = labelName.equals(REPEATED_FIELD_LABEL);
        return result;
    }

    private MethodSpec createNewBuilderMethod() {
        final ClassName builderClass = getBuilderClassName();
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

    private Class<?> getParameterClass(FieldDescriptorProto fieldDescriptor) {
        String typeName = fieldDescriptor.getTypeName();
        if (typeName.isEmpty()) {
            return GenerationUtils.getType(fieldDescriptor.getType()
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
