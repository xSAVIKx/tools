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

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.RootDoc;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.spine3.gradle.protobuf.failure.Given.FailuresGenerationConfigurer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.spine3.gradle.TaskName.COMPILE_JAVA;
import static org.spine3.gradle.protobuf.failure.Given.FailuresJavadocConfigurer;
import static org.spine3.gradle.protobuf.failure.Given.FailuresJavadocConfigurer.TEST_SOURCE;
import static org.spine3.gradle.protobuf.failure.Given.FailuresJavadocConfigurer.getExpectedClassComment;
import static org.spine3.gradle.protobuf.failure.Given.FailuresJavadocConfigurer.getExpectedCtorComment;
import static org.spine3.gradle.protobuf.javadoc.JavadocEscaper.EscapedStrings.AMPERSAND;
import static org.spine3.gradle.protobuf.javadoc.JavadocEscaper.EscapedStrings.AT_MARK;
import static org.spine3.gradle.protobuf.javadoc.JavadocEscaper.EscapedStrings.BACK_SLASH;
import static org.spine3.gradle.protobuf.javadoc.JavadocEscaper.EscapedStrings.COMMENT_BEGINNING;
import static org.spine3.gradle.protobuf.javadoc.JavadocEscaper.EscapedStrings.COMMENT_ENDING;
import static org.spine3.gradle.protobuf.javadoc.JavadocEscaper.EscapedStrings.GREATER_THAN;
import static org.spine3.gradle.protobuf.javadoc.JavadocEscaper.EscapedStrings.LESS_THAN;
import static org.spine3.gradle.protobuf.javadoc.JavadocEscaper.escape;

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

        final ProjectConnection connection =
                new FailuresGenerationConfigurer(testProjectDir).configure();
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

        final ProjectConnection connection
                = new FailuresJavadocConfigurer(testProjectDir).configure();
        final BuildLauncher launcher = connection.newBuild();

        launcher.forTasks(
                COMPILE_JAVA.getValue()
        );
        try {
            launcher.run(new ResultHandler<Void>() {
                @Override
                public void onComplete(Void aVoid) {
                    final RootDoc root = RootDocReceiver.getRootDoc(testProjectDir, TEST_SOURCE);
                    final ClassDoc failureDoc = root.classes()[0];
                    final ConstructorDoc failureCtorDoc = failureDoc.constructors()[0];

                    assertEquals(getExpectedClassComment(), failureDoc.getRawCommentText());
                    assertEquals(getExpectedCtorComment(), failureCtorDoc.getRawCommentText());
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
        assertEquals(COMMENT_ENDING.getEscaped(), escape(COMMENT_ENDING.getUnescaped()));
        assertEquals(' ' + COMMENT_BEGINNING.getEscaped(),
                     escape(' ' + COMMENT_BEGINNING.getUnescaped()));
    }

    @Test
    public void escape_slash_in_beginning() {
        assertEquals("&#47;", escape("/"));
    }

    @Test
    public void escape_html() {
        assertEquals(LESS_THAN.getEscaped(), escape(LESS_THAN.getUnescaped()));
        assertEquals(GREATER_THAN.getEscaped(), escape(GREATER_THAN.getUnescaped()));
        assertEquals(AMPERSAND.getEscaped(), escape(AMPERSAND.getUnescaped()));
    }

    @Test
    public void escape_at_and_back_slash() {
        assertEquals(AT_MARK.getEscaped(), escape(AT_MARK.getUnescaped()));
        assertEquals(BACK_SLASH.getEscaped(), escape(BACK_SLASH.getUnescaped()));
    }
}
