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
package org.spine3.gradle.protobuf.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;

/**
 * Utilities for working with Protobuf when generating Java code.
 *
 * @author Alexander Yevsyukov
 */
public class JavaCode {

    private JavaCode() {
    }

    /**
     * Calculates a name of an outer Java class for types declared in the file represented
     * by the passed descriptor.
     *
     * <p>The outer class name is calculated according to
     * <a href="https://developers.google.com/protocol-buffers/docs/reference/java-generated#invocation">Protobuf
     * compiler conventions</a>.
     *
     * @param descriptor a descriptor for file for which outer class name will be generated
     * @return non-qualified outer class name
     */
    public static String getOuterClassName(FileDescriptorProto descriptor) {
        String outerClassNameFromOptions = descriptor.getOptions()
                                                     .getJavaOuterClassname();
        if (!outerClassNameFromOptions.isEmpty()) {
            return outerClassNameFromOptions;
        }

        final String fullFileName = descriptor.getName();
        final int lastBackslashIndex = fullFileName.lastIndexOf('/');
        final int extensionIndex = descriptor.getName()
                                             .lastIndexOf(".proto");
        final String fileName = fullFileName.substring(lastBackslashIndex + 1, extensionIndex);
        final String className = toCamelCase(fileName);
        return className;
    }

    /**
     * Transforms the string with a file name with underscores into a camel-case name.
     *
     * <p>The class name is calculated according to
     * <a href="https://developers.google.com/protocol-buffers/docs/reference/java-generated#invocation">Protobuf
     * compiler conventions</a>.
     */
    @VisibleForTesting // otherwise it would have been private
    public static String toCamelCase(String fileName) {
        final StringBuilder result = new StringBuilder(fileName.length());

        for (final String word : fileName.split("_")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                result.append(word.substring(1)
                                  .toLowerCase());
            }
        }

        return result.toString();
    }
}
