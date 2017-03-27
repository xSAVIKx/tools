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

package org.spine3.gradle.protobuf.failure;

import com.sun.javadoc.RootDoc;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Test projects configurers.
 *
 * @author Dmytro Grankin
 */
@SuppressWarnings("UtilityClass")
class Given {

    private static final Class<Given> cls = Given.class;

    private Given() {
    }

    static class FailuresGenerationConfigurer extends ProjectConfigurer {

        private static final String PROJECT_NAME = "failures-gen-plugin-test/";
        private static final String[] TEST_PROTO_FILES = {
                "test_failures.proto",
                "outer_class_by_file_name_failures.proto",
                "outer_class_set_failures.proto",
                "deps/deps.proto"
        };

        FailuresGenerationConfigurer(TemporaryFolder projectDirectory) {
            super(projectDirectory);
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

    static class FailuresJavadocConfigurer extends ProjectConfigurer {

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
        static final String TEST_SOURCE = "/generated/main/spine/org/spine3/sample/failures/"
                + FAILURE_NAME + ".java";

        FailuresJavadocConfigurer(TemporaryFolder projectDirectory) {
            super(projectDirectory);
        }

        @Override
        public ProjectConnection configure() throws IOException {
            writeBuildGradle();
            writeTestSource();
            return createProjectConnection();
        }

        private void writeTestSource() throws IOException {
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
                                    .resolve(BASE_PROTO_LOCATION + FAILURES_FILE_NAME);
            Files.createDirectories(sourcePath.getParent());
            Files.write(sourcePath, sourceLines, Charset.forName("UTF-8"));
        }

        static String getExpectedClassComment() {
            return ' ' + FailureJavadocGenerator.OPENING_PRE + JAVADOC_LINE_SEPARATOR
                    + ' ' + CLASS_COMMENT + JAVADOC_LINE_SEPARATOR
                    + " </pre>" + JAVADOC_LINE_SEPARATOR + JAVADOC_LINE_SEPARATOR
                    + " Failure based on protobuf type {@code " + JAVA_PACKAGE + '.' + FAILURE_NAME
                    + '}' + JAVADOC_LINE_SEPARATOR;
        }

        static String getExpectedCtorComment() {
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
    abstract static class ProjectConfigurer {

        private static final String BUILD_GRADLE_NAME = "build.gradle";
        static final String BASE_PROTO_LOCATION = "src/main/proto/spine/sample/failures/";

        @SuppressWarnings("PackageVisibleField") // Inheritance in same file.
        final TemporaryFolder projectDirectory;

        ProjectConfigurer(TemporaryFolder projectDirectory) {
            this.projectDirectory = projectDirectory;
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
            final String protoFilePath = BASE_PROTO_LOCATION + protoFile;

            final Path resultingPath = projectDirectory.getRoot()
                                                       .toPath()
                                                       .resolve(protoFilePath);
            final InputStream fileContent = cls.getClassLoader()
                                               .getResourceAsStream(projectName + protoFilePath);

            Files.createDirectories(resultingPath.getParent());
            Files.copy(fileContent, resultingPath);
        }
    }
}
