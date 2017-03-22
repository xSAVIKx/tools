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

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configurers for the test projects.
 *
 * @author Dmytro Grankin
 */
class Configurers {

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

        static final String TEST_SOURCE = "/generated/main/spine/org/spine3/sample/failures/Failure.java";
        private static final String PROJECT_NAME = "failures-javadoc-test/";
        private static final String FAILURES_FILE = "javadoc_failures.proto";

        FailuresJavadocConfigurer(TemporaryFolder projectDirectory) {
            super(projectDirectory);
        }

        @Override
        public ProjectConnection configure() throws IOException {
            writeBuildGradle();
            writeProto(PROJECT_NAME, FAILURES_FILE);

            return createProjectConnection();
        }
    }

    abstract static class ProjectConfigurer {

        private static final String BUILD_GRADLE_NAME = "build.gradle";
        private static final String BASE_PROTO_LOCATION = "src/main/proto/spine/sample/failures/";

        private final TemporaryFolder projectDirectory;

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
            final InputStream fileContent = Configurers.class.getClassLoader()
                                                             .getResourceAsStream(BUILD_GRADLE_NAME);

            Files.createDirectories(resultingPath.getParent());
            Files.copy(fileContent, resultingPath);
        }

        void writeProto(String projectName, String protoFile) throws IOException {
            final String protoFilePath = BASE_PROTO_LOCATION + protoFile;

            final Path resultingPath = projectDirectory.getRoot()
                                                       .toPath()
                                                       .resolve(protoFilePath);
            final InputStream fileContent = Configurers.class.getClassLoader()
                                                             .getResourceAsStream(projectName + protoFilePath);

            Files.createDirectories(resultingPath.getParent());
            Files.copy(fileContent, resultingPath);
        }
    }
}
