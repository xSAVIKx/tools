/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
package org.spine3.tools.javadoc.fqnchecker;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.spine3.gradle.TaskName;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FqnCheckPluginShould {

    private String resourceFolder = "";
    private static final String SOURCE_FOLDER = "src/main/java";
    private final String checkFqnTask = TaskName.CHECK_FQN.getValue();

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Before
    public void setUpTestProject() throws IOException {
        final Path buildFile = testProjectDir.getRoot()
                                             .toPath()
                                             .resolve("build.gradle");
        final InputStream buildFileContent =
                getClass().getClassLoader()
                          .getResourceAsStream("projects/JavaDocCheckerPlugin/build.gradle");


        final Path testSources = testProjectDir.getRoot()
                                               .toPath()
                                               .resolve(SOURCE_FOLDER);
        Files.copy(buildFileContent, buildFile);
        Files.createDirectories(testSources);

        ClassLoader classLoader = getClass().getClassLoader();
        final String testFile = "AllowedFQNformat.java";
        final String resourceFilePath = classLoader.getResource(testFile)
                                                   .getPath();
        resourceFolder = resourceFilePath.substring(0, resourceFilePath.length() - testFile.length());
    }

    @Test
    public void fail_build_if_wrong_fqn_name_found() throws IOException {
        final Path testSources = testProjectDir.getRoot()
                                               .toPath()
                                               .resolve(SOURCE_FOLDER);
        FileUtils.copyDirectory(new File(resourceFolder), new File(testSources.toString()));


        BuildResult buildResult = GradleRunner.create()
                                              .withProjectDir(testProjectDir.getRoot())
                                              .withPluginClasspath()
                                              .withArguments(checkFqnTask)
                                              .buildAndFail();

        assertTrue(buildResult.getOutput().contains("Wrong link found"));
    }

    @Test
    public void allow_correct_fqn_name_format() throws IOException {
        final Path testSources = testProjectDir.getRoot()
                                               .toPath()
                                               .resolve(SOURCE_FOLDER);
        final Path wrongFqnFormat = Paths.get(testSources.toString() + "/WrongFQNformat.java");
        FileUtils.copyDirectory(new File(resourceFolder), new File(testSources.toString()));
        Files.deleteIfExists(wrongFqnFormat);

        BuildResult buildResult = GradleRunner.create()
                                              .withProjectDir(testProjectDir.getRoot())
                                              .withPluginClasspath()
                                              .withArguments(checkFqnTask)
                                              .build();

        final List<String> expected = Arrays.asList(":compileJava", ":checkFqn");

        assertEquals(expected, extractTasks(buildResult));
    }

    private static List<String> extractTasks(BuildResult buildResult) {
        return FluentIterable
                .from(buildResult.getTasks())
                .transform(new Function<BuildTask, String>() {
                    @Override
                    public String apply(BuildTask buildTask) {
                        return buildTask.getPath();
                    }
                })
                .toList();
    }
}
