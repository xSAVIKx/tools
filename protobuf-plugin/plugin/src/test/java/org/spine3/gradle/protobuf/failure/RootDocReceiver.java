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
import com.sun.tools.doclets.standard.Standard;
import com.sun.tools.javadoc.Main;
import org.junit.rules.TemporaryFolder;

/**
 * {@link RootDoc} receiver for the tests purposes.
 *
 * <p>This could be achieved by extending {@link Standard} doclet
 * and defining static method with the following signature:
 * <pre>{@code public static boolean start(RootDoc}</pre>
 *
 * @author Dmytro Grankin
 */
@SuppressWarnings("ExtendsUtilityClass")
public class RootDocReceiver extends Standard {

    /**
     * Should be received only by {@link #getRootDoc(TemporaryFolder, String)} call,
     * that guarantee proper initialization.
     */
    @SuppressWarnings("StaticVariableMayNotBeInitialized")
    private static RootDoc rootDoc;

    /**
     * Returns {@link RootDoc} for the specified source.
     *
     * <p>Executes {@link #main(String[])}, which in turn
     * call {@link Main#execute(String, String, String...)}.
     * Such call chain guarantee proper {@link #rootDoc} initialization.
     *
     * @param projectDir the project directory, that contains the source
     * @param source     the source relative location
     * @return the root document
     */
    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    static RootDoc getRootDoc(TemporaryFolder projectDir, String source) {
        main(new String[]{
                projectDir.getRoot()
                          .getAbsolutePath() + source
        });
        return rootDoc;
    }

    public static void main(String[] args) {
        final String name = RootDocReceiver.class.getName();
        Main.execute(name, name, args);
    }

    /**
     * Return {@code true} anyway, because used just to receive {@link RootDoc}.
     *
     * <p>Called by {@link Main#execute(String, String, String...)}
     *
     * @param root the {@link RootDoc} formed by {@link Main#execute(String, String, String...)}
     * @return {@code true} anyway
     */
    @SuppressWarnings("unused")
    public static boolean start(RootDoc root) {
        rootDoc = root;
        return true;
    }
}
