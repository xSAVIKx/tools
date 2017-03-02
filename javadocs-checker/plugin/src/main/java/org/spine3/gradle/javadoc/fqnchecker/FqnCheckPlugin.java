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
package org.spine3.gradle.javadoc.fqnchecker;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.regex.Pattern.compile;
import static org.spine3.gradle.TaskName.CHECK_FQN;
import static org.spine3.gradle.TaskName.COMPILE_JAVA;
import static org.spine3.gradle.TaskName.PROCESS_RESOURCES;
import static org.spine3.gradle.javadoc.Extension.getDirsToCheck;

/**
 * @author Alexander Aleksandrov
 */
public class FqnCheckPlugin extends SpinePlugin {
    @Override
    public void apply(final Project project) {
        final Action<Task> checkJavadocAction = checkJavadocActionFor(project);
        newTask(CHECK_FQN, checkJavadocAction).insertAfterTask(COMPILE_JAVA)
                                              .insertBeforeTask(PROCESS_RESOURCES)
                                              .applyNowTo(project);
        log().debug("Started to check Javadocs {}", checkJavadocAction);
    }

    private static Action<Task> checkJavadocActionFor(final Project project) {
        log().debug("Checking files");
        return new Action<Task>() {
            @Override
            public void execute(Task task) {
                final List<String> dirsToCheck = getDirsToCheck(project);
                findFqnLinksWithoutText(dirsToCheck);
            }
        };
    }

    private static void findFqnLinksWithoutText(List<String> pathsToDirs) {
        for (String dirPath : pathsToDirs) {
            final File file = new File(dirPath);
            if (file.exists()) {
                checkRecursively(file.toPath());
            } else {
                log().debug("No more files left to check");
            }
        }
    }

    private static void checkRecursively(Path path) {
        try {
            final SimpleFileVisitor<Path> visitor = new RecursiveFileChecker();
            log().debug("Starting to check the files recursively in {}", path.toString());
            Files.walkFileTree(path, visitor);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to check the folder with its contents: " + path, e);
        }
    }

    /**
     * Custom {@linkplain java.nio.file.FileVisitor visitor} which recursively deletes the contents of the walked folder.
     */
    // A completely different behavior for the visitor methods is required.
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    private static class RecursiveFileChecker extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            log().debug("Performing FQN check for the file: {}", file);
            check(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            log().error("Error walking down the file tree for file: {}", file);
            return FileVisitResult.TERMINATE;
        }
    }

    static void check(Path file) throws InvalidFqnUsageException {
        final byte[] rawContent;
        try {
            rawContent = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read the contents of the file: " + file, e);
        }
        final String content = new String(rawContent);
        final Optional<InvalidFqnUsage> checkResult = check(content);
        if (checkResult.isPresent()) {
            final String message = "Wrong FQN name of the link was found: " +
                    checkResult.get()
                               .getActualUsage() + " in :" + file;
            log().error(message);
            throw new InvalidFqnUsageException(file.toFile()
                                                   .getAbsolutePath(), message);
        }
    }

    @VisibleForTesting
    static Optional<InvalidFqnUsage> check(String content) {
        String left = content;

        final String commentStart = CommentMarker.START.getValue();
        final String commentEnd = CommentMarker.END.getValue();

        while (left.contains(commentStart)) {
            final Optional<String> substring = substringBetween(left,
                                                                commentStart,
                                                                commentEnd);
            if (substring.isPresent()) {
                final String comment = substring.get();
                final Optional<InvalidFqnUsage> result = checkSingleComment(comment);
                if (result.isPresent()) {
                    return result;
                }
                left = left.substring(comment.length() + commentStart.length() + commentEnd.length());
            }
            left = substringStartsWith(left, commentStart);

        }
        return Optional.absent();
    }

    private static Optional<InvalidFqnUsage> checkSingleComment(String comment) {
        final Matcher matcher = JavadocPattern.LINK.getPattern()
                                                   .matcher(comment);
        boolean found = matcher.find();
        if (found) {
            final String improperUsage = matcher.group(0);
            final InvalidFqnUsage result = new InvalidFqnUsage(improperUsage);
            return Optional.of(result);
        }
        return Optional.absent();
    }

    private static String substringStartsWith(String str, String separator) {
        if (str.isEmpty()) {
            return str;
        } else if (separator == null) {
            return "";
        } else {
            int pos = str.indexOf(separator);
            return pos == -1 ? "" : str.substring(pos);
        }
    }

    private static Optional<String> substringBetween(String str, String open, String close) {
        checkNotNull(str);
        checkNotNull(open);
        checkNotNull(close);

        final int start = str.indexOf(open);
        if (start != -1) {
            final int end = str.indexOf(close, start + open.length());
            if (end != -1) {
                final String result = str.substring(start + open.length(), end);
                return Optional.of(result);
            }
        }
        return Optional.absent();
    }

    private enum JavadocPattern {

        LINK(compile("(\\{@link|\\{@linkplain) *(org\\.)([a-zA-Z]|[0-9]*|\\.*)*(\\}|\\ *\\})"));

        private final Pattern pattern;

        JavadocPattern(Pattern pattern) {
            this.pattern = pattern;
        }

        public Pattern getPattern() {
            return pattern;
        }
    }

    private enum CommentMarker {

        START("/*"),
        END("*/");

        private final String value;

        CommentMarker(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FqnCheckPlugin.class);
    }
}

