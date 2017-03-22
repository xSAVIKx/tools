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

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.RootDoc;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.System.lineSeparator;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.spine3.gradle.TaskName.COMPILE_JAVA;
import static org.spine3.gradle.protobuf.failures.FailureJavadocGenerator.EscapedCharacters.AMPERSAND;
import static org.spine3.gradle.protobuf.failures.FailureJavadocGenerator.EscapedCharacters.ASTERISK;
import static org.spine3.gradle.protobuf.failures.FailureJavadocGenerator.EscapedCharacters.AT_MARK;
import static org.spine3.gradle.protobuf.failures.FailureJavadocGenerator.EscapedCharacters.BACK_SLASH;
import static org.spine3.gradle.protobuf.failures.FailureJavadocGenerator.EscapedCharacters.GREATER_THAN;
import static org.spine3.gradle.protobuf.failures.FailureJavadocGenerator.EscapedCharacters.LESS_THAN;
import static org.spine3.gradle.protobuf.failures.FailureJavadocGenerator.EscapedCharacters.SLASH;
import static org.spine3.gradle.protobuf.failures.FailureJavadocGenerator.escapeJavadoc;

/**
 * @author Dmytro Grankin
 */
public class FailuresGenPluginShould {

    @SuppressWarnings("PublicField") // Rules should be public
    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Test
    public void compile_generated_failures() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        setUpFailuresGenPluginTestProject();
        final ProjectConnection connection = createProjectConnection();
        final BuildLauncher launcher = connection.newBuild();

        launcher.forTasks(
                COMPILE_JAVA.getValue()
        );
        try {
            launcher.run(new ResultHandler<Void>() {
                @Override
                public void onComplete(Void aVoid) {
                    // Test passed.
                    countDownLatch.countDown();
                }

                @SuppressWarnings("CallToPrintStackTrace") // Used for easier debugging.
                @Override
                public void onFailure(GradleConnectionException e) {
                    e.printStackTrace();
                    fail("Tasks execution should not failed.");
                }
            });
        } finally {
            connection.close();
        }

        countDownLatch.await(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void generate_failure_javadoc() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        setUpFailuresJavadocTestProject();
        final ProjectConnection connection = createProjectConnection();
        final BuildLauncher launcher = connection.newBuild();

        launcher.forTasks(
                COMPILE_JAVA.getValue()
        );
        try {
            launcher.run(new ResultHandler<Void>() {
                @Override
                public void onComplete(Void aVoid) {
                    final String sourceToTest = "/generated/main/spine/org/spine3/sample/failures/Failure.java";
                    final RootDoc root = RootDocReceiver.getRootDoc(testProjectDir, sourceToTest);
                    final ClassDoc failureDoc = root.classes()[0];
                    final ConstructorDoc failureCtorDoc = failureDoc.constructors()[0];

                    final String expectedClassComment = "<pre>" + lineSeparator()
                            + "  The failure definition to test Javadoc generation." + lineSeparator()
                            + " </pre>" + lineSeparator() + lineSeparator()
                            + " Failure based on protobuf type {@code org.spine3.sample.failures.Failure}";
                    final String expectedCtorComment = " Creates a new instance." + lineSeparator() + lineSeparator()
                            + " @param id      the failure ID" + lineSeparator()
                            + " @param message the failure message" + lineSeparator();

                    assertEquals(expectedClassComment, failureDoc.commentText());
                    assertEquals(expectedCtorComment, failureCtorDoc.getRawCommentText());

                    countDownLatch.countDown();
                }

                @SuppressWarnings("CallToPrintStackTrace") // Used for easier debugging.
                @Override
                public void onFailure(GradleConnectionException e) {
                    e.printStackTrace();
                    fail("Sources compilation failed");
                }
            });
        } finally {
            connection.close();
        }

        countDownLatch.await(100, TimeUnit.MILLISECONDS);
    }

    @Test
    public void escape_comment_beginning_and_ending() {
        assertEquals(" /" + ASTERISK.getEscapedString(), escapeJavadoc(" /*"));
        assertEquals('*' + SLASH.getEscapedString(), escapeJavadoc("*/"));
    }

    @Test
    public void not_escape_just_asterisk_and_slash() {
        assertEquals("*", escapeJavadoc("*"));
        assertEquals(" /", escapeJavadoc(" /"));
    }

    @Test
    public void escape_html() {
        assertEquals(LESS_THAN.getEscapedString(), escapeJavadoc("<"));
        assertEquals(GREATER_THAN.getEscapedString(), escapeJavadoc(">"));
        assertEquals(AMPERSAND.getEscapedString(), escapeJavadoc("&"));
    }

    @Test
    public void escape_at_and_back_slash() {
        assertEquals(AT_MARK.getEscapedString(), escapeJavadoc("@"));
        assertEquals(BACK_SLASH.getEscapedString(), escapeJavadoc("\\"));
    }

    private ProjectConnection createProjectConnection() {
        final GradleConnector connector = GradleConnector.newConnector();
        connector.forProjectDirectory(testProjectDir.getRoot());
        return connector.connect();
    }

    private void setUpFailuresGenPluginTestProject() throws IOException {
        final String projectName = "failures-gen-plugin-test/";
        writeCommonBuildGradle();
        writeProto(projectName, "test_failures.proto");
        writeProto(projectName, "outer_class_by_file_name_failures.proto");
        writeProto(projectName, "outer_class_set_failures.proto");
        writeProto(projectName, "deps/deps.proto");
    }

    private void setUpFailuresJavadocTestProject() throws IOException {
        final String projectName = "failures-javadoc-test/";
        writeCommonBuildGradle();
        writeProto(projectName, "javadoc_failures.proto");
    }

    private void writeCommonBuildGradle() throws IOException {
        final String commonBuildGradleName = "build.gradle";
        final Path resultingPath = testProjectDir.getRoot()
                                                 .toPath()
                                                 .resolve(commonBuildGradleName);
        final InputStream fileContent = getClass().getClassLoader()
                                                  .getResourceAsStream(commonBuildGradleName);

        Files.createDirectories(resultingPath.getParent());
        Files.copy(fileContent, resultingPath);
    }

    private void writeProto(String projectName, String protoFile) throws IOException {
        final String baseProtoLocation = "src/main/proto/spine/sample/failures/";
        final String protoFilePath = baseProtoLocation + protoFile;

        final Path resultingPath = testProjectDir.getRoot()
                                                 .toPath()
                                                 .resolve(protoFilePath);
        final InputStream fileContent = getClass().getClassLoader()
                                                  .getResourceAsStream(projectName + protoFilePath);

        Files.createDirectories(resultingPath.getParent());
        Files.copy(fileContent, resultingPath);
    }
}
