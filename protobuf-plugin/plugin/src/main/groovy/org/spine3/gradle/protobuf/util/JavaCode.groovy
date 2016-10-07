package org.spine3.gradle.protobuf.util

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;

/**
 * Utilities for working with Protobuf when generating Java code.
 *
 * @author Alexander Yevsyukov
 */
class JavaCode {

    //TODO:2016-10-07:alexander.yevsyukov: This method duplicates getJavaOuterClassName
    static GString getOuterClassName(FileDescriptorProto descriptor) {
        GString classname = "$descriptor.options.javaOuterClassname"
        if (!classname.isEmpty()) {
            return "$classname"
        }
        classname = "${descriptor.name.substring(descriptor.name.lastIndexOf('/') + 1, descriptor.name.lastIndexOf(".proto"))}"
        final GString result = "${classname.charAt(0).toUpperCase()}${classname.substring(1)}"
        return result
    }

    static String getJavaOuterClassName(FileDescriptorProto descriptor) {
        String outerClassNameFromOptions = descriptor.options.javaOuterClassname
        if (!outerClassNameFromOptions.isEmpty()) {
            return outerClassNameFromOptions;
        }

        final String fullFileName = descriptor.getName()
        final int lastBackslash = fullFileName.lastIndexOf('/')
        final String onlyName = fullFileName.substring(lastBackslash + 1)
                                            .replace(".proto", "")
        return toCamelCase(onlyName)
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
