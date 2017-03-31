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

package org.spine3.gradle.protobuf;

import com.sun.javadoc.RootDoc;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import static org.spine3.gradle.TaskName.CLEAN;
import static org.spine3.gradle.TaskName.COMPILE_JAVA;
import static org.spine3.gradle.TaskName.COMPILE_TEST_JAVA;
import static org.spine3.gradle.TaskName.GENERATE_PROTO;
import static org.spine3.gradle.TaskName.GENERATE_TEST_PROTO;
import static org.spine3.gradle.TaskName.PROCESS_RESOURCES;
import static org.spine3.gradle.TaskName.PROCESS_TEST_RESOURCES;

/**
 * A helper class for the test data generation.
 *
 * @author Dmytro Grankin
 */
@SuppressWarnings("UtilityClass")
public class Given {

    private static final Class<Given> cls = Given.class;
    private static final String BASE_FAILURE_PROTO_LOCATION = "src/main/proto/spine/sample/failures/";
    static final String SPINE_PROTOBUF_PLUGIN_ID = "org.spine3.tools.protobuf-plugin";

    // prevent instantiation of this utility class
    private Given() {
    }

    /** Creates a project with all required tasks. */
    static Project newProject() {
        final Project project = ProjectBuilder.builder()
                                              .build();
        project.task(CLEAN.getValue());
        project.task(GENERATE_PROTO.getValue());
        project.task(GENERATE_TEST_PROTO.getValue());
        project.task(COMPILE_JAVA.getValue());
        project.task(COMPILE_TEST_JAVA.getValue());
        project.task(PROCESS_RESOURCES.getValue());
        project.task(PROCESS_TEST_RESOURCES.getValue());
        return project;
    }

    static String newUuid() {
        final String result = UUID.randomUUID()
                                  .toString();
        return result;
    }

    public static class FailuresGenerationConfigurer extends ProjectConfigurer {

        private static final String PROJECT_NAME = "failures-gen-plugin-test/";
        private static final String[] TEST_PROTO_FILES = {
                "test_failures.proto",
                "outer_class_by_file_name_failures.proto",
                "outer_class_set_failures.proto",
                "deps/deps.proto"
        };

        public FailuresGenerationConfigurer(TemporaryFolder projectDirectory) {
            super(projectDirectory, BASE_FAILURE_PROTO_LOCATION);
        }

        @Override
        public ProjectConnection configure() throws IOException {
            writeBuildGradle();
            for (String protoFile : TEST_PROTO_FILES) {
                writeProto(PROJECT_NAME, protoFile);
            }

            return createProjectConnection();
        }
    }

    public static class ValidatorsGenerationConfigurer extends ProjectConfigurer {

        private static final String PROJECT_NAME = "validators-gen-plugin-test/";
        private static final String[] TEST_PROTO_FILES = {
                "identifiers.proto",
                "attributes.proto",
                "changes.proto",
                "c/commands.proto"
        };

        public ValidatorsGenerationConfigurer(TemporaryFolder projectDirectory) {
            super(projectDirectory, "src/main/proto/spine/sample/validators/");
        }

        @Override
        public ProjectConnection configure() throws IOException {
            writeBuildGradle();
            for (String protoFile : TEST_PROTO_FILES) {
                writeProto(PROJECT_NAME, protoFile);
            }

            return createProjectConnection();
        }
    }

    public static class FailuresJavadocConfigurer extends ProjectConfigurer {

        /** Javadocs received from {@link RootDoc} contain "\n" line separator. */
        @SuppressWarnings("HardcodedLineSeparator")
        private static final String JAVADOC_LINE_SEPARATOR = "\n";

        private static final String JAVA_PACKAGE = "org.spine3.sample.failures";
        private static final String CLASS_COMMENT =
                "The failure definition to test Javadoc generation.";
        private static final String FAILURE_NAME = "Failure";
        private static final String FAILURES_FILE_NAME = "javadoc_failures.proto";
        private static final String FIRST_FIELD_COMMENT = "The failure ID.";
        private static final String FIRST_FIELD_NAME = "id";
        private static final String SECOND_FIELD_COMMENT = "The failure message.";
        private static final String SECOND_FIELD_NAME = "message";
        public static final String TEST_SOURCE = "/generated/main/spine/org/spine3/sample/failures/"
                + FAILURE_NAME + ".java";

        public FailuresJavadocConfigurer(TemporaryFolder projectDirectory) {
            super(projectDirectory, BASE_FAILURE_PROTO_LOCATION);
        }

        @Override
        public ProjectConnection configure() throws IOException {
            writeBuildGradle();
            writeFailureProto();
            return createProjectConnection();
        }

        private void writeFailureProto() throws IOException {
            final Iterable<String> sourceLines = Arrays.asList(
                    "syntax = \"proto3\";",
                    "package spine.sample.failures;",
                    "option java_package = \"" + JAVA_PACKAGE + "\";",
                    "option java_multiple_files = false;",

                    "//" + CLASS_COMMENT,
                    "message " + FAILURE_NAME + " {",

                    "//" + FIRST_FIELD_COMMENT,
                    "int32 " + FIRST_FIELD_NAME + " = 1; // Is not a part of Javadoc.",

                    "//" + SECOND_FIELD_COMMENT,
                    "string " + SECOND_FIELD_NAME + " = 2;",

                    "bool hasNoComment = 3;",
                    "}"

            );

            final Path sourcePath =
                    projectDirectory.getRoot()
                                    .toPath()
                                    .resolve(getBaseProtoLocation() + FAILURES_FILE_NAME);
            Files.createDirectories(sourcePath.getParent());
            Files.write(sourcePath, sourceLines, Charset.forName("UTF-8"));
        }

        public static String getExpectedClassComment() {
            return ' ' + "<pre>" + JAVADOC_LINE_SEPARATOR
                    + ' ' + CLASS_COMMENT + JAVADOC_LINE_SEPARATOR
                    + " </pre>" + JAVADOC_LINE_SEPARATOR + JAVADOC_LINE_SEPARATOR
                    + " Failure based on protobuf type {@code " + JAVA_PACKAGE + '.' + FAILURE_NAME
                    + '}' + JAVADOC_LINE_SEPARATOR;
        }

        public static String getExpectedCtorComment() {
            final String param = " @param ";
            return " Creates a new instance." + JAVADOC_LINE_SEPARATOR + JAVADOC_LINE_SEPARATOR
                    + param + FIRST_FIELD_NAME + "      " + FIRST_FIELD_COMMENT
                    + JAVADOC_LINE_SEPARATOR
                    + param + SECOND_FIELD_NAME + ' ' + SECOND_FIELD_COMMENT
                    + JAVADOC_LINE_SEPARATOR;
        }
    }

    /**
     * Abstract base for configuring a {@linkplain TemporaryFolder test project directory}
     * and receiving {@link ProjectConnection}.
     */
    public abstract static class ProjectConfigurer {

        private static final String BUILD_GRADLE_NAME = "build.gradle";
        private final String baseProtoLocation;

        @SuppressWarnings("PackageVisibleField") // Inheritance in same file.
        final TemporaryFolder projectDirectory;

        ProjectConfigurer(TemporaryFolder projectDirectory, String baseProtoLocation) {
            this.projectDirectory = projectDirectory;
            this.baseProtoLocation = baseProtoLocation;
        }

        public abstract ProjectConnection configure() throws IOException;

        ProjectConnection createProjectConnection() {
            final GradleConnector connector = GradleConnector.newConnector();
            connector.forProjectDirectory(projectDirectory.getRoot());
            return connector.connect();
        }

        void writeBuildGradle() throws IOException {
            final Path resultingPath = projectDirectory.getRoot()
                                                       .toPath()
                                                       .resolve(BUILD_GRADLE_NAME);
            final InputStream fileContent = cls.getClassLoader()
                                               .getResourceAsStream(BUILD_GRADLE_NAME);

            Files.createDirectories(resultingPath.getParent());
            Files.copy(fileContent, resultingPath);
        }

        void writeProto(String projectName, String protoFile) throws IOException {
            final String protoFilePath = baseProtoLocation + protoFile;

            final Path resultingPath = projectDirectory.getRoot()
                                                       .toPath()
                                                       .resolve(protoFilePath);
            final InputStream fileContent = cls.getClassLoader()
                                               .getResourceAsStream(projectName + protoFilePath);

            Files.createDirectories(resultingPath.getParent());
            Files.copy(fileContent, resultingPath);
        }

        String getBaseProtoLocation() {
            return baseProtoLocation;
        }
    }
}
