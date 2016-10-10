package org.spine3.gradle.protobuf.util

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;

/**
 * Utilities for working with Protobuf when generating Java code.
 *
 * @author Alexander Yevsyukov
 */
class JavaCode {

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

    /**
     * Transforms the string with a file name with underscores into a camel-case name.
     *
     * <p>The class name is calculated according to
     * <a href="https://developers.google.com/protocol-buffers/docs/reference/java-generated#invocation">Protobuf
     * compiler conventions</a>.
     */
    @VisibleForTesting // otherwise it would have been private
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
