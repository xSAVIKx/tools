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

package org.spine3.gradle.protobuf.validators;

import com.google.common.io.Files;
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
import org.spine3.gradle.protobuf.validators.methods.MethodConstructorFactory;
import org.spine3.gradle.protobuf.validators.methods.RepeatedFieldMethodsConstructor;
import org.spine3.gradle.protobuf.validators.methods.SettersConstructor;
import org.spine3.server.validate.AbstractValidatingBuilder;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.gradle.protobuf.GenerationUtils.getJavaFieldName;
import static org.spine3.gradle.protobuf.GenerationUtils.isRepeated;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getParameterClass;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getValidatingBuilderGenericClassName;
import static org.spine3.gradle.protobuf.validators.methods.MethodConstructorFactory.createPrivateConstructor;

/**
 * Class, which writes Validator java code, based on it's descriptor.
 *
 * @author Illia Shepilov
 */
class ValidatorWriter {

    private static final String REPEATED_FIELD_LABEL = "LABEL_REPEATED";

    private final String javaClass;
    private final String javaPackage;
    private final DescriptorProto descriptor;
    private final MessageTypeCache messageTypeCache;
    private final ClassName builderGenericClassName;
    private final String targetDir;
    private final MethodConstructorFactory constructorFactory;

    ValidatorWriter(WriterDto writerDto, String targetDir, MessageTypeCache messageTypeCache) {
        this.javaClass = writerDto.getJavaClass();
        this.javaPackage = writerDto.getJavaPackage();
        this.descriptor = writerDto.getMsgDescriptor();
        this.messageTypeCache = messageTypeCache;
        this.targetDir = targetDir;
        builderGenericClassName = getValidatingBuilderGenericClassName(javaPackage, messageTypeCache, descriptor.getName());
        constructorFactory = new MethodConstructorFactory(writerDto, messageTypeCache);
    }

    void write() {
        log().debug(String.format("Writing the %s under %s", javaClass, javaPackage));
        final File rootDirectory = new File(targetDir);

        final List<MethodSpec> methods = newArrayList();

        methods.add(createPrivateConstructor());
        methods.add(constructorFactory.createNewBuilderMethod());
        methods.add(constructorFactory.createBuildMethod());

        methods.addAll(createSetters());
        methods.addAll(createRepeatedFieldMethods());

        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(javaClass);
        final Collection<FieldSpec> fields = getFields();
        addFields(classBuilder, fields);
        addMethods(classBuilder, methods);

        final TypeSpec javaClass = classBuilder.build();

        log().debug(String.format("Writing %s class to the %s directory", javaClass, rootDirectory));
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
            if (isRepeated(fieldDescriptor)) {
                continue;
            }
            final SettersConstructor constructor =
                    SettersConstructor.newBuilder()
                                      .setFieldDescriptor(fieldDescriptor)
                                      .setFieldIndex(index)
                                      .setJavaClass(javaClass)
                                      .setJavaPackage(javaPackage)
                                      .setBuilderGenericClassName(builderGenericClassName)
                                      .setMessageTypeCache(messageTypeCache)
                                      .build();
            final Collection<MethodSpec> methods = constructor.construct();
            setters.addAll(methods);
            ++index;
        }
        return setters;
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
                                                       .setBuilderGenericClass(builderGenericClassName)
                                                       .setMessageTypeCache(messageTypeCache)
                                                       .build();
                final Collection<MethodSpec> repeatedFieldMethods = constructor.construct();
                methods.addAll(repeatedFieldMethods);
            }
            ++fieldIndex;
        }
        return methods;
    }

    private void writeClass(File rootFolder, TypeSpec validator) {
        try {
            Files.touch(rootFolder);
            JavaFile.builder(javaPackage, validator)
                    .build()
                    .writeTo(rootFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TypeSpec.Builder addMethods(TypeSpec.Builder builder, Iterable<MethodSpec> methodSpecs) {
        final ClassName abstractBuilderTypeName = ClassName.get(AbstractValidatingBuilder.class);
        final ParameterizedTypeName superClass =
                ParameterizedTypeName.get(abstractBuilderTypeName, builderGenericClassName);
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
               .superclass(superClass)
               .addMethods(methodSpecs);
        return builder;
    }

    private class FieldConstructor {

        private final FieldDescriptorProto fieldDescriptor;

        private FieldConstructor(FieldDescriptorProto fieldDescriptor) {
            this.fieldDescriptor = fieldDescriptor;
        }

        public FieldSpec construct() {
            if (isRepeated(fieldDescriptor)) {
                final FieldSpec result = constructRepeatedField();
                return result;
            }

            final FieldSpec result = constructField();
            return result;
        }

        private FieldSpec constructRepeatedField() {
            final ClassName rawType = ClassName.get(List.class);
            final ParameterizedTypeName param =
                    ParameterizedTypeName.get(rawType, getParameterClass(fieldDescriptor, messageTypeCache));
            final String fieldName = getJavaFieldName(fieldDescriptor.getName(), false);
            return FieldSpec.builder(param, fieldName, Modifier.PRIVATE)
                            .build();
        }

        private FieldSpec constructField() {
            final String fieldName = getJavaFieldName(fieldDescriptor.getName(), false);
            final ClassName fieldClass = getParameterClass(fieldDescriptor, messageTypeCache);
            final FieldSpec result = FieldSpec.builder(fieldClass, fieldName, Modifier.PRIVATE)
                                              .build();
            return result;
        }
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
