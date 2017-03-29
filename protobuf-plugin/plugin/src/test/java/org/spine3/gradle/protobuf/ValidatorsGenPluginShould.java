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

import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;
import static junit.framework.TestCase.fail;
import static org.spine3.gradle.TaskName.COMPILE_JAVA;

/**
 * @author Illia Shepilov
 */
public class ValidatorsGenPluginShould {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        writeFile("build.gradle");
        writeProto("attributes.proto");
        writeProto("changes.proto");
        writeProto("identifiers.proto");
        writeProto("c/commands.proto");
    }

    @SuppressWarnings("BusyWait")
    @Test
    public void compile_generated_validators() throws InterruptedException {
        final AtomicBoolean testCompleted = new AtomicBoolean(false);

        final GradleConnector connector = GradleConnector.newConnector();
        connector.forProjectDirectory(testProjectDir.getRoot());
        final ProjectConnection connection = connector.connect();
        final BuildLauncher launcher = connection.newBuild();

        launcher.forTasks(
                COMPILE_JAVA.getValue()
        );
        try {
            launcher.run(new ResultHandler<Void>() {
                @Override
                public void onComplete(Void aVoid) {
                    // Test passed.
                    testCompleted.set(true);
                }

                @SuppressWarnings("CallToPrintStackTrace") // Used for easier debugging.
                @Override
                public void onFailure(GradleConnectionException e) {
                    e.printStackTrace();
                    fail("Task should finish successfully.");
                }
            });
        } finally {
            connection.close();
        }

        while (!testCompleted.get()) {
            sleep(100);
        }
    }

    private void writeProto(String protoFile) throws IOException {
        final String baseProtoLocation = "src/main/proto/";
        writeFile(baseProtoLocation + protoFile);
    }

    private void writeFile(String file) throws IOException {
        final String projectName = "validators-gen-plugin-test/";
        final Path resultingPath = testProjectDir.getRoot()
                                                 .toPath()
                                                 .resolve(file);
        final InputStream fileContent = getClass().getClassLoader()
                                                  .getResourceAsStream(projectName + file);

        Files.createDirectories(resultingPath.getParent());
        Files.copy(fileContent, resultingPath);
    }
}
