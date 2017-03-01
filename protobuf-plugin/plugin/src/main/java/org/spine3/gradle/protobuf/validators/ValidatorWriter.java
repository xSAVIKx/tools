package org.spine3.gradle.protobuf.validators;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.FieldPath;
import org.spine3.gradle.protobuf.GenerationUtils;
import org.spine3.gradle.protobuf.MessageTypeCache;
import org.spine3.server.validate.FieldValidator;
import org.spine3.server.validate.FieldValidatorFactory;
import org.spine3.server.validate.ValidatingBuilder;
import org.spine3.validate.ConstraintViolation;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import static org.gradle.internal.impldep.com.beust.jcommander.internal.Lists.newArrayList;
import static org.spine3.gradle.protobuf.GenerationUtils.getJavaFieldName;

/**
 * Class, which writes Validator java code, based on it's descriptor.
 *
 * @author Illia Shepilov
 */
public class ValidatorWriter {

    private static final Pattern COMPILE = Pattern.compile("\\.");
    private static final String rootFolder = "generated/main/java/";
    private final MessageTypeCache messageTypeCache;
    private final WriterDto writerDto;

    public ValidatorWriter(WriterDto writerDto, MessageTypeCache messageTypeCache) {
        this.writerDto = writerDto;
        this.messageTypeCache = messageTypeCache;
    }

    public void write() {
        log().debug("Writing the java class under {}", writerDto.getJavaPackage());
        final File rootDirecroy = new File(rootFolder);

        final DescriptorProto descriptor = writerDto.getMsgDescriptor();
        final List<FieldSpec> fields = getFields();

        final List<MethodSpec> methods = newArrayList();
        methods.add(createNewBuilderMethod(descriptor));
        methods.addAll(createSetters(descriptor));
        methods.addAll(createRawSetters(descriptor));
        methods.add(createBuildMethod(descriptor));
        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(writerDto.getJavaClass());
        final MethodSpec constructor = createConstructor();
        methods.add(constructor);
        addFields(classBuilder, fields);
        addMethods(classBuilder, methods);
        final TypeSpec classToWrite = classBuilder.build();

        try {
            writeClass(rootDirecroy, classToWrite);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<FieldSpec> getFields() {
        final List<FieldSpec> fields = newArrayList();
        for (FieldDescriptorProto fieldDescriptor : writerDto.getMsgDescriptor()
                                                             .getFieldList()) {
            final String fieldName = getJavaFieldName(fieldDescriptor.getName(), false);
            final FieldSpec field = FieldSpec.builder(getParameterClass(fieldDescriptor),
                                                      fieldName,
                                                      Modifier.PRIVATE)
                                             .build();
            fields.add(field);
        }
        return fields;
    }

    private static TypeSpec.Builder addFields(TypeSpec.Builder classBuilder, List<FieldSpec> fields) {
        return classBuilder.addFields(fields);
    }

    private List<MethodSpec> createSetters(DescriptorProto descriptorProto) {
        final List<MethodSpec> setters = newArrayList();
        for (FieldDescriptorProto descriptorField : descriptorProto.getFieldList()) {
            Class<?> parameterClass = getParameterClass(descriptorField);
            final String paramName = getJavaFieldName(descriptorField.getName(), false);
            final ParameterSpec parameter = ParameterSpec.builder(parameterClass, paramName)
                                                         .build();
            final String fieldName = getJavaFieldName(paramName, false);
            final String setterPart = getJavaFieldName(paramName, true);
            final String methodName = "set" + setterPart;
            final MethodSpec methodSpec = MethodSpec.methodBuilder(methodName)
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .addParameter(parameter)
                                                    .addException(IOException.class) //TODO:2017-03-01:illiashepilov: set correct exception
                                                    .addStatement("final $T fieldPath = $T.newBuilder().addFieldName($S).build()", FieldPath.class, FieldPath.class, fieldName)
                                                    .addStatement("final $T<?> validator = $T.createStrict(descriptorField, " + fieldName + ", fieldPath)", FieldValidator.class, FieldValidatorFactory.class)
                                                    .addStatement("final $T<$T> constraints = validator.validate()", List.class, ConstraintViolation.class)
                                                    .beginControlFlow("if(!constraints.isEmpty())")
                                                    .addStatement("throw new $T(constraints)", RuntimeException.class)//TODO:2017-03-01:illiashepilov: set ValidationFailure exception
                                                    .endControlFlow()
                                                    .addStatement("this." + fieldName + " = " + fieldName)
                                                    .build();
            setters.add(methodSpec);
        }
        return setters;
    }

    private List<MethodSpec> createRawSetters(DescriptorProto descriptorProto) {
        final List<MethodSpec> setters = newArrayList();
        for (FieldDescriptorProto descriptorField : descriptorProto.getFieldList()) {
            Class<?> parameterClass = getParameterClass(descriptorField);
            final String paramName = getJavaFieldName(descriptorField.getName(), false);
            final ParameterSpec parameter = ParameterSpec.builder(parameterClass, paramName)
                                                         .build();
            final String fieldName = getJavaFieldName(paramName, false);
            final String setterPart = getJavaFieldName(paramName, true);
            final String methodName = "set" + setterPart + "Raw";
            final MethodSpec methodSpec = MethodSpec.methodBuilder(methodName)
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .addParameter(parameter)
                                                    .addException(IOException.class) //TODO:2017-03-01:illiashepilov: set correct exception
                                                    .addException(SQLException.class) //TODO:2017-03-01:illiashepilov: set correct exception
                                                    .addStatement("final $T fieldPath = $T.newBuilder().addFieldName($S).build()", FieldPath.class, FieldPath.class, descriptorField.getName())
                                                    .addStatement("final $T<?> validator = $T.createStrict(descriptorField, " + fieldName + ", fieldPath)", FieldValidator.class, FieldValidatorFactory.class)
                                                    .addStatement("final $T<$T> constraints = validator.validate()", List.class, ConstraintViolation.class)
                                                    .beginControlFlow("if(!constraints.isEmpty())")
                                                    .addStatement("throw new $T(constraints)", RuntimeException.class)//TODO:2017-03-01:illiashepilov: set ValidationFailure exception
                                                    .endControlFlow()
                                                    .addStatement("this." + fieldName + " = " + fieldName)
                                                    .build();
            setters.add(methodSpec);
        }
        return setters;
    }

    private MethodSpec createBuildMethod(DescriptorProto descriptorProto) {
        final StringBuilder builder = new StringBuilder("final $T result = $T.newBuilder()");
        for (FieldDescriptorProto fieldDescription : descriptorProto.getFieldList()) {
            builder.append(".")
                   .append("set")
                   .append(getJavaFieldName(fieldDescription.getName(), true))
                   .append("(")
                   .append(getJavaFieldName(fieldDescription.getName(), false))
                   .append(")");
        }
        builder.append(".build()");

        final ClassName builtMessage = ClassName.get(writerDto.getJavaPackage(), descriptorProto.getName());
        final MethodSpec buildMethod = MethodSpec.methodBuilder("build")
                                                 .addModifiers(Modifier.PUBLIC)
                                                 .returns(builtMessage)
                                                 .addException(IOException.class) //TODO:2017-03-01:illiashepilov: set correct exception
                                                 .addException(SQLException.class) //TODO:2017-03-01:illiashepilov: set correct exception
                                                 .addStatement(builder.toString(), builtMessage, builtMessage)
                                                 .addStatement("return result")
                                                 .build();
        return buildMethod;
    }

    private MethodSpec createNewBuilderMethod(DescriptorProto descriptorProto) {
        final ClassName thatClass = ClassName.get(writerDto.getJavaPackage(), writerDto.getJavaClass());
        final MethodSpec buildMethod = MethodSpec.methodBuilder("newBuilder")
                                                 .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                 .returns(thatClass)
                                                 .addStatement("return new $T()", thatClass)
                                                 .build();
        return buildMethod;
    }

    private Class<?> getParameterClass(FieldDescriptorProto descriptorField) {
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

    private void writeClass(File rootFodler, TypeSpec validator) throws IOException {
        JavaFile javaFile = JavaFile.builder(writerDto.getJavaPackage(), validator)
                                    .build();
        javaFile.writeTo(rootFodler);
    }

    private static MethodSpec createConstructor() {
        final MethodSpec result = MethodSpec.constructorBuilder()
                                            .addModifiers(Modifier.PRIVATE)
                                            .build();
        return result;
    }

    private static TypeSpec.Builder addMethods(TypeSpec.Builder builder, Iterable<MethodSpec> methodSpecs) {
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
               .addSuperinterface(ValidatingBuilder.class)
               .addMethods(methodSpecs);
        return builder;
    }

    private static String toJavaPath(String path) {
        final String result = COMPILE.matcher(path)
                                     .replaceAll(String.valueOf(File.separatorChar));
        return result;
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
