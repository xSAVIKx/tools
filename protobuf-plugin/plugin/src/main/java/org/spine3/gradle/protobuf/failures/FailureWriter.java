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
package org.spine3.gradle.protobuf.failures;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.GeneratedMessageV3;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.CommandContext;
import org.spine3.base.FailureThrowable;
import org.spine3.gradle.protobuf.failures.fieldtype.FieldType;
import org.spine3.gradle.protobuf.failures.fieldtype.FieldTypeFactory;

import javax.annotation.Generated;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static org.spine3.gradle.protobuf.failures.FailureWriter.FailureThrowableCtorParams.*;

/**
 * Class, which writes Failure java code, based on it's descriptor.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
@SuppressWarnings("HardcodedLineSeparator")
public class FailureWriter {

    private static final String COMMA_SEPARATOR = ", ";

    private final DescriptorProto failureDescriptor;
    private final File outputDirectory;
    private final String javaPackage;

    private final String outerClassName;
    private final String className;

    private final FieldTypeFactory fieldTypeFactory;

    /**
     * Creates a new instance.
     *
     * @param failureDescriptor {@link DescriptorProto} of failure's proto message
     * @param outputDirectory   a {@linkplain File directory} to write a Failure
     * @param javaPackage       Failure's java package
     * @param messageTypeMap    pre-scanned map with proto types and their appropriate Java classes
     */
    public FailureWriter(DescriptorProto failureDescriptor,
                         File outputDirectory,
                         String javaPackage,
                         String javaOuterClassName,
                         Map<String, String> messageTypeMap) {
        this.failureDescriptor = failureDescriptor;
        this.outputDirectory = outputDirectory;
        this.javaPackage = javaPackage;
        this.outerClassName = javaOuterClassName;
        this.className = failureDescriptor.getName();
        this.fieldTypeFactory = new FieldTypeFactory(failureDescriptor, messageTypeMap);
    }

    /**
     * Initiates writing.
     */
    void write() {
        try {
            log().debug("Creating the output directory {}", outputDirectory.getPath());
            Files.createDirectories(outputDirectory.toPath());

            log().debug("Constructing {}", className);
            final TypeSpec failure = TypeSpec.classBuilder(className)
                                             .addAnnotation(constructGeneratedAnnotation())
                                             .addModifiers(PUBLIC)
                                             .superclass(FailureThrowable.class)
                                             .addField(constructSerialVersionUID())
                                             .addMethod(constructConstructor())
                                             .addMethod(constructGetFailureMessage())
                                             .build();
            final JavaFile javaFile = JavaFile.builder(javaPackage, failure)
                                              .build();
            log().debug("Writing {}", className);
            javaFile.writeTo(outputDirectory);
            log().debug("Failure {} written successfully", className);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private MethodSpec constructConstructor() {
        log().debug("Constructing the constructor of type '{}'", failureDescriptor.getName());
        final MethodSpec.Builder builder = constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(GeneratedMessageV3.class, COMMAND_MESSAGE.getName())
                .addParameter(CommandContext.class, COMMAND_CONTEXT.getName())
                .addParameter(GeneratedMessageV3.class, FAILURE_MESSAGE.getName());
        for (Map.Entry<String, FieldType> field : readFieldValues().entrySet()) {
            final TypeName parameterTypeName = field.getValue()
                                                    .getTypeName();
            final String parameterName = getJavaFieldName(field.getKey(), false);
            builder.addParameter(parameterTypeName, parameterName);
        }

        return builder.addStatement(getSuperStatement())
                      .build();
    }

    private String getSuperStatement() {
        final StringBuilder superStatement = new StringBuilder(
                "super(" + COMMAND_MESSAGE.getName() + COMMA_SEPARATOR
                        + COMMAND_CONTEXT.getName() + COMMA_SEPARATOR
                        + outerClassName + '.' + className + ".newBuilder()");

        for (Map.Entry<String, FieldType> field : readFieldValues().entrySet()) {
            final String upperCaseName = getJavaFieldName(field.getKey(), true);
            superStatement.append('.')
                          .append(field.getValue()
                                       .getSetterPrefix())
                          .append(upperCaseName)
                          .append('(')
                          .append(getJavaFieldName(field.getKey(), false))
                          .append(')');
        }
        superStatement.append(".build())");

        return superStatement.toString();
    }

    private MethodSpec constructGetFailureMessage() {
        log().debug("Constructing getFailureMessage()");

        final TypeName returnTypeName = ClassName.get(outerClassName, className);
        return MethodSpec.methodBuilder("getFailureMessage")
                         .addAnnotation(Override.class)
                         .addModifiers(PUBLIC)
                         .returns(returnTypeName)
                         .addStatement("return (" + returnTypeName + ") super.getFailureMessage()")
                         .build();
    }

    private static AnnotationSpec constructGeneratedAnnotation() {
        return AnnotationSpec.builder(Generated.class)
                             .addMember("value", "$S", "by Spine compiler")
                             .build();
    }

    private static FieldSpec constructSerialVersionUID() {
        return FieldSpec.builder(long.class, "serialVersionUID", PRIVATE, STATIC, FINAL)
                        .initializer("0L")
                        .build();
    }

    /**
     * Transforms Protobuf-style field name into corresponding Java-style field name.
     *
     * <p>For example, seat_assignment_id -> SeatAssignmentId
     *
     * @param protoFieldName  Protobuf field name.
     * @param capitalizeFirst Indicates if we need first letter of the output to be capitalized.
     * @return a field name
     */
    private static String getJavaFieldName(String protoFieldName, boolean capitalizeFirst) {
        final String[] words = protoFieldName.split("_");
        final StringBuilder builder = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            final String word = words[i];
            builder.append(Character.toUpperCase(word.charAt(0)))
                   .append(word.substring(1));
        }
        String resultName = builder.toString();
        if (capitalizeFirst) {
            resultName = Character.toUpperCase(resultName.charAt(0)) + resultName.substring(1);
        }
        return resultName;
    }

    /**
     * Reads all descriptor fields.
     *
     * @return name-to-{@link FieldType} map
     */
    private Map<String, FieldType> readFieldValues() {
        log().debug("Reading all the field values from the descriptor: {}", failureDescriptor);

        final Map<String, FieldType> result = new LinkedHashMap<>();
        for (FieldDescriptorProto field : failureDescriptor.getFieldList()) {
            result.put(field.getName(), fieldTypeFactory.create(field));
        }
        log().debug("Read fields: {}", result);

        return result;
    }

    enum FailureThrowableCtorParams {
        COMMAND_MESSAGE("commandMessage"),
        COMMAND_CONTEXT("commandContext"),
        FAILURE_MESSAGE("failureMessage");

        private final String name;

        FailureThrowableCtorParams(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static Logger log() {
        return LoggerSingleton.INSTANCE.logger;
    }

    private enum LoggerSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger logger = LoggerFactory.getLogger(FailureWriter.class);
    }

}
