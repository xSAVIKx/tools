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

import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;
import com.sun.tools.javadoc.Main;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("ExtendsUtilityClass") // Should extend doclet to receive RootDoc
public class RootDocReceiver extends Standard {

    @SuppressWarnings("StaticVariableMayNotBeInitialized") // Initialized in start method.
    private static RootDoc rootDoc;

    @SuppressWarnings("StaticVariableUsedBeforeInitialization") // Initialized in start method.
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

    @SuppressWarnings("unused") // called by com.sun.tools.javadoc.Main
    public static boolean start(RootDoc root) {
        rootDoc = root;
        return true;
    }
}
