package org.spine3.gradle.protobuf.validators;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.gradle.protobuf.MessageTypeCache;
import org.spine3.gradle.protobuf.validators.methods.RepeatedFieldMethodsConstructor;
import org.spine3.gradle.protobuf.validators.methods.SettersConstructor;
import org.spine3.server.validate.AbstractValidatingBuilder;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.gradle.internal.impldep.com.beust.jcommander.internal.Lists.newArrayList;
import static org.spine3.gradle.protobuf.GenerationUtils.getJavaFieldName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.ADD_ALL_PREFIX;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.SETTER_PREFIX;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getBuilderClassName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getParameterClass;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getValidatingBuilderGenericClass;

/**
 * Class, which writes Validator java code, based on it's descriptor.
 *
 * @author Illia Shepilov
 */
class ValidatorWriter {

    private static final String ROOT_FOLDER = "generated/main/java/";
    private static final String REPEATED_FIELD_LABEL = "LABEL_REPEATED";

    private final String javaClass;
    private final String javaPackage;
    private final DescriptorProto descriptor;
    private final MessageTypeCache messageTypeCache;
    private final Class<?> builderGenericClass;

    ValidatorWriter(WriterDto writerDto, MessageTypeCache messageTypeCache) {
        this.javaClass = writerDto.getJavaClass();
        this.javaPackage = writerDto.getJavaPackage();
        this.descriptor = writerDto.getMsgDescriptor();
        this.messageTypeCache = messageTypeCache;
        builderGenericClass = getValidatingBuilderGenericClass(javaPackage, messageTypeCache, descriptor.getName());
    }

    void write() {
        log().debug("Writing the %s under %s", javaClass, javaPackage);
        final File rootDirectory = new File(ROOT_FOLDER);
        final List<MethodSpec> methods = newArrayList();

        final MethodSpec constructor = createConstructor();
        methods.add(constructor);
        methods.add(createNewBuilderMethod());
        methods.addAll(createSetters());
        methods.add(createBuildMethod());
        methods.addAll(createRepeatedFieldMethods());

        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(javaClass);
        final Collection<FieldSpec> fields = getFields();
        addFields(classBuilder, fields);
        addMethods(classBuilder, methods);

        final TypeSpec javaClass = classBuilder.build();
        writeClass(rootDirectory, javaClass);
    }

    private Collection<FieldSpec> getFields() {
        final List<FieldSpec> fields = newArrayList();
        for (FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {
            fields.add(new FieldConstructor(fieldDescriptor).construct());
        }
        return fields;
    }

    private static TypeSpec.Builder addFields(TypeSpec.Builder classBuilder, Iterable<FieldSpec> fields) {
        return classBuilder.addFields(fields);
    }

    private Collection<MethodSpec> createSetters() {
        final List<MethodSpec> setters = newArrayList();
        int index = 0;
        for (FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {
            if (isRepeatedField(fieldDescriptor)) {
                continue;
            }
            final SettersConstructor constructor = SettersConstructor.newBuilder()
                                                                     .setFieldDescriptor(fieldDescriptor)
                                                                     .setFieldIndex(index)
                                                                     .setJavaClass(javaClass)
                                                                     .setJavaPackage(javaPackage)
                                                                     .setBuilderGenericClass(builderGenericClass)
                                                                     .setMessageTypeCache(messageTypeCache)
                                                                     .build();
            final Collection<MethodSpec> methods = constructor.construct();
            setters.addAll(methods);
            ++index;
        }
        return setters;
    }

    private class FieldConstructor {

        private final FieldDescriptorProto fieldDescriptor;

        private FieldConstructor(FieldDescriptorProto fieldDescriptor) {
            this.fieldDescriptor = fieldDescriptor;
        }

        public FieldSpec construct() {
            if (isRepeatedField(fieldDescriptor)) {
                final FieldSpec result = constructRepeatedField();
                return result;
            }

            final FieldSpec result = constructField();
            return result;
        }

        private FieldSpec constructRepeatedField() {
            final ParameterizedTypeName param =
                    ParameterizedTypeName.get(List.class, getParameterClass(fieldDescriptor, messageTypeCache));
            final String fieldName = getJavaFieldName(fieldDescriptor.getName(), false);
            return FieldSpec.builder(param, fieldName, Modifier.PRIVATE)
                            .build();
        }

        private FieldSpec constructField() {
            final String fieldName = getJavaFieldName(fieldDescriptor.getName(), false);
            final Class<?> fieldClass = getParameterClass(fieldDescriptor, messageTypeCache);
            final FieldSpec result = FieldSpec.builder(fieldClass, fieldName, Modifier.PRIVATE)
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
                final RepeatedFieldMethodsConstructor constructor =
                        RepeatedFieldMethodsConstructor.newBuilder()
                                                       .setFieldDescriptor(fieldDescriptor)
                                                       .setFieldIndex(fieldIndex)
                                                       .setJavaClass(javaClass)
                                                       .setJavaPackage(javaPackage)
                                                       .setBuilderGenericClass(builderGenericClass)
                                                       .setMessageTypeCache(messageTypeCache)
                                                       .build();
                final Collection<MethodSpec> repeatedFieldMethods = constructor.construct();
                methods.addAll(repeatedFieldMethods);
            }
            ++fieldIndex;
        }
        return methods;
    }

    private MethodSpec createBuildMethod() {
        final StringBuilder builder = new StringBuilder("final $T result = $T.newBuilder()");
        for (FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {
            builder.append('.');

            if (isRepeatedField(fieldDescriptor)) {
                builder.append(ADD_ALL_PREFIX);
            } else {
                builder.append(SETTER_PREFIX);
            }

            builder.append(getJavaFieldName(fieldDescriptor.getName(), true))
                   .append('(')
                   .append(getJavaFieldName(fieldDescriptor.getName(), false))
                   .append(')');
        }
        builder.append(".build()");

        final MethodSpec buildMethod = MethodSpec.methodBuilder("build")
                                                 .addModifiers(Modifier.PUBLIC)
                                                 .returns(builderGenericClass)
                                                 .addStatement(builder.toString(),
                                                               builderGenericClass,
                                                               builderGenericClass)
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
        final ClassName builderClass = getBuilderClassName(javaPackage, javaClass);
        final MethodSpec buildMethod = MethodSpec.methodBuilder("newBuilder")
                                                 .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                 .returns(builderClass)
                                                 .addStatement("return new $T()", builderClass)
                                                 .build();
        return buildMethod;
    }

    private void writeClass(File rootFolder, TypeSpec validator) {
        try {
            JavaFile.builder(javaPackage, validator)
                    .build()
                    .writeTo(rootFolder);
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
                ParameterizedTypeName.get(AbstractValidatingBuilder.class, builderGenericClass);
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
