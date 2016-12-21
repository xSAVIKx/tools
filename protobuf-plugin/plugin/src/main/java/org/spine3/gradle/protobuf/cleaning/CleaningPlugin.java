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
package org.spine3.gradle.protobuf.cleaning;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.gradle.SpinePlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static org.spine3.gradle.TaskName.CLEAN;
import static org.spine3.gradle.TaskName.PRE_CLEAN;
import static org.spine3.gradle.protobuf.Extension.getDirsToClean;

/**
 * Plugin which performs additional cleanup of the Spine-generated folders.
 *
 * <p>Adds a custom `:preClean` task, which is executed before the `:clean` task.
 *
 * @author Mikhail Mikhaylov
 * @author Alex Tymchenko
 */
public class CleaningPlugin extends SpinePlugin {

    @Override
    public void apply(final Project project) {
        final Action<Task> preCleanAction = new Action<Task>() {
            @Override
            public void execute(Task task) {
                deleteDirs(getDirsToClean(project));
            }
        };
        final GradleTask preCleanTask = newTask(PRE_CLEAN, preCleanAction).insertBeforeTask(CLEAN)
                                                                          .applyNowTo(project);
        log().debug("Pre-clean phase initialized: {}", preCleanTask);
    }

    private static void deleteDirs(List<String> dirs) {
        for (String dirPath : dirs) {
            final File file = new File(dirPath);
            if (file.exists() && file.isDirectory()) {
                deleteRecursively(file.toPath());
            }
        }
    }

    private static void deleteRecursively(Path path) {
        try {
            final SimpleFileVisitor<Path> visitor = new RecursiveDirectoryCleaner();
            Files.walkFileTree(path, visitor);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete the folder with its contents: " + path, e);
        }
    }

    /**
     * Custom {@link java.nio.file.FileVisitor} which recursively deletes the contents of the walked folder.
     */
    @SuppressWarnings("RefusedBequest")     // As we define a completely different behavior for the visitor methods.
    private static class RecursiveDirectoryCleaner extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
            if (e == null) {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            } else {
                throw e;
            }
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(CleaningPlugin.class);
    }
}
