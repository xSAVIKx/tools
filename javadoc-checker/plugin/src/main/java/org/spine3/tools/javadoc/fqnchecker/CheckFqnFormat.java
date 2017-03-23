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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.regex.Pattern.compile;

/**
 * @author Alexander Aleksandrov
 */
public class CheckFqnFormat {
    private int exceptionThreshold = 0;
    private static final String DIRECTORY_TO_CHECK = "/src/main/java";
    private String reactionType = "";
    private final Project project;
    private static final InvalidResultStorage storage = new InvalidResultStorage();

    public CheckFqnFormat(Project project) {
        this.project = project;
    }

    public Action<Task> actionFor(final Project project) {
        log().debug("Preparing an action for Javadock checker");
        return new Action<Task>() {
            @Override
            public void execute(Task task) {
                exceptionThreshold = Extension.getThreshold(project);
                reactionType = Extension.getReactionType(project);

                final List<String> dirsToCheck = getDirsToCheck(project);
                findFqnLinksWithoutText(dirsToCheck);
                log().debug("Ending an action");
            }
        };
    }

    private static List<String> getDirsToCheck(Project project) {
        log().debug("Finding the directories to check");
        final String mainScopeJavaFolder = project.getProjectDir()
                                                  .getAbsolutePath() + DIRECTORY_TO_CHECK;
        final List<String> result = newArrayList(mainScopeJavaFolder);
        log().debug("{} directories found for the check: {}", result.size(), result);

        return result;
    }

    private void findFqnLinksWithoutText(List<String> pathsToDirs) {
        for (String path : pathsToDirs) {
            final File file = new File(path);
            if (file.exists()) {
                checkRecursively(file.toPath());
            } else {
                log().debug("No more files left to check");
            }
        }
    }

    private void checkRecursively(Path path) {
        try {
            final SimpleFileVisitor<Path> visitor = new CheckFqnFormat.RecursiveFileChecker();
            log().debug("Starting to check the files recursively in {}", path.toString());
            Files.walkFileTree(path, visitor);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to check the folder with its contents: " +
                                                    path, e);
        }
    }

    /**
     * Custom {@linkplain java.nio.file.FileVisitor visitor} which recursively checks
     * the contents of the walked folder.
     */
    // A completely different behavior for the visitor methods is required.

    private class RecursiveFileChecker extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            super.visitFile(path, attrs);
            log().debug("Performing FQN check for the file: {}", path);
            check(path);
            return FileVisitResult.CONTINUE;
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            log().error("Error walking down the file tree for file: {}", file);
            return FileVisitResult.TERMINATE;
        }
    }

    private void check(Path path) throws InvalidFqnUsageException {
        final List<String> content;
        if (!path.toString()
                 .endsWith(".java")) {
            return;
        }
        try {
            content = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(" Cannot read the contents of the file: " + path, e);
        }
        final List<Optional<InvalidFqnUsage>> invalidLinks = check(content);

        if (!invalidLinks.isEmpty()) {
            storage.save(path, invalidLinks);
            if (storage.getLinkTotal() > exceptionThreshold) {
                final String message =
                        " Links with fully-qualified names should be in format {@link <FQN> <text>}" +
                        " or {@linkplain <FQN> <text>}.";
                log().error(message);
                storage.logInvalidFqnUsages();

                if (reactionType.equals("error")) {
                    throw new InvalidFqnUsageException(path.toFile()
                                                           .getAbsolutePath(), message);
                }
            }
        }
    }

    @VisibleForTesting
    private static List<Optional<InvalidFqnUsage>> check(List<String> content) {
        int lineNumber = 0;
        final List<Optional<InvalidFqnUsage>> invalidLinks = newArrayList();
        for (String line : content) {
            final Optional<InvalidFqnUsage> result = checkSingleComment(line);
            lineNumber++;
            if (result.isPresent()) {
                result.get().setIndex(lineNumber);
                invalidLinks.add(result);
            }
        }
        return invalidLinks;
    }

    private static Optional<InvalidFqnUsage> checkSingleComment(String comment) {
        final Matcher matcher = CheckFqnFormat.JavadocPattern.LINK.getPattern()
                                                                  .matcher(comment);
        final boolean found = matcher.find();
        if (found) {
            final String improperUsage = matcher.group(0);
            final InvalidFqnUsage result = new InvalidFqnUsage(improperUsage);
            return Optional.of(result);
        }
        return Optional.absent();
    }

    private enum JavadocPattern {

        LINK(compile("(\\{@link|\\{@linkplain) *((?!-)[a-z0-9-]{1,63}\\.)((?!-)[a-zA-Z0-9-]{1,63}[a-zA-Z0-9-]\\.)+[a-zA-Z]{2,63}(\\}|\\ *\\})"));

        private final Pattern pattern;

        JavadocPattern(Pattern pattern) {
            this.pattern = pattern;
        }

        public Pattern getPattern() {
            return pattern;
        }
    }

    private static Logger log() {
        return CheckFqnFormat.LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(CheckFqnFormat.class);
    }

}
