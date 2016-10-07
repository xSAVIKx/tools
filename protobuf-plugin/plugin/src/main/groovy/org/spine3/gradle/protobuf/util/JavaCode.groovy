package org.spine3.gradle.protobuf.util

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;

/**
 * Utilities for working with Protobuf when generating Java code.
 *
 * @author Alexander Yevsyukov
 */
class JavaCode {

    static String getOuterClassName(FileDescriptorProto descriptor) {
        String outerClassNameFromOptions = descriptor.options.javaOuterClassname
        if (!outerClassNameFromOptions.isEmpty()) {
            return outerClassNameFromOptions;
        }

        final String fullFileName = descriptor.name
        final int lastBackslashIndex = fullFileName.lastIndexOf('/')
        final int extensionIndex = descriptor.name.lastIndexOf(".proto");
        final String fileName = fullFileName.substring(lastBackslashIndex + 1, extensionIndex)
        final String className = toCamelCase(fileName)
        return className
    }

    static String toCamelCase(String fileName) {
        final StringBuilder result = new StringBuilder(fileName.length());

        for (final String word : fileName.split("_")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                result.append(word.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }
}
