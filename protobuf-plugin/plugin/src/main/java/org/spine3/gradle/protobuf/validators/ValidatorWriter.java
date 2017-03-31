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
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.gradle.protobuf.message.MessageTypeCache;
import org.spine3.gradle.protobuf.validators.construction.FieldConstructor;
import org.spine3.gradle.protobuf.validators.construction.MethodConstructorManager;
import org.spine3.server.validate.AbstractValidatingBuilder;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;

import static org.spine3.gradle.protobuf.util.GenerationUtils.constructGeneratedAnnotation;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getValidatorGenericClassName;
import static org.spine3.util.Exceptions.newIllegalArgumentException;

/**
 * Class which writes generated validators to the Java files.
 *
 * @author Illia Shepilov
 */
class ValidatorWriter {

    private final String javaClass;
    private final String targetDir;
    private final String javaPackage;
    private final ClassName builderGenericClassName;
    private final FieldConstructor fieldConstructor;
    private final MethodConstructorManager methodConstructorManager;

    ValidatorWriter(ValidatorMetadata validatorMetadata,
                    String targetDir,
                    MessageTypeCache messageTypeCache) {
        this.javaClass = validatorMetadata.getJavaClass();
        this.javaPackage = validatorMetadata.getJavaPackage();
        this.targetDir = targetDir;
        this.methodConstructorManager = new MethodConstructorManager(validatorMetadata,
                                                                     messageTypeCache);

        final DescriptorProto descriptor = validatorMetadata.getMsgDescriptor();
        this.builderGenericClassName = getValidatorGenericClassName(javaPackage,
                                                                    messageTypeCache,
                                                                    descriptor.getName());
        this.fieldConstructor = new FieldConstructor(messageTypeCache, descriptor);
    }

    /**
     * Writes the generated validator to the Java file.
     */
    void write() {
        final String debugMsg = String.format("Writing the %s class under the %s package",
                                              javaClass, javaPackage);
        log().debug(debugMsg);

        final File rootDirectory = new File(targetDir);
        final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(javaClass);
        addFields(classBuilder, fieldConstructor.construct());
        addMethods(classBuilder, methodConstructorManager.createMethods());
        final TypeSpec javaClass = classBuilder.addAnnotation(constructGeneratedAnnotation())
                                               .build();
        writeClass(rootDirectory, javaClass);

        log().debug("Class was written.");
    }

    private static TypeSpec.Builder addFields(TypeSpec.Builder classBuilder,
                                              Iterable<FieldSpec> fields) {
        final TypeSpec.Builder result = classBuilder.addFields(fields);
        return result;
    }

    private TypeSpec.Builder addMethods(TypeSpec.Builder builder,
                                        Iterable<MethodSpec> methodSpecs) {
        final ClassName abstractBuilderTypeName = ClassName.get(AbstractValidatingBuilder.class);
        final ParameterizedTypeName superClass =
                ParameterizedTypeName.get(abstractBuilderTypeName, builderGenericClassName);
        builder.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
               .superclass(superClass)
               .addMethods(methodSpecs);
        return builder;
    }

    private void writeClass(File rootFolder, TypeSpec validator) {
        try {
            Files.touch(rootFolder);
            JavaFile.builder(javaPackage, validator)
                    .build()
                    .writeTo(rootFolder);
        } catch (IOException e) {
            final String exMessage = String.format("%s was not written.", rootFolder);
            throw newIllegalArgumentException(exMessage ,e);
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
