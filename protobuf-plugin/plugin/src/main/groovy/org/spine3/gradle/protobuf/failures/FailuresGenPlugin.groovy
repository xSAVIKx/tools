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

package org.spine3.gradle.protobuf.failures

import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import static com.google.protobuf.DescriptorProtos.*
import static org.spine3.gradle.protobuf.Extension.*
import static org.spine3.gradle.protobuf.util.DescriptorSetUtil.getProtoFileDescriptors
/**
 * Plugin which generates Failures, based on failures.proto files.
 *
 * <p>Uses generated proto descriptors.
 * <p>Logs a warning if there are no protobuf descriptors generated.
 */
@Slf4j
class FailuresGenPlugin implements Plugin<Project> {

    private Project project

    /** A map from Protobuf type name to Java class FQN. */
    private Map<GString, GString> cachedMessageTypes = new HashMap<>()

    /**
     * Applied to project.
     *
     * <p>Adds :generateFailures and :generateTestFailures tasks.
     * <p>Tasks depend on corresponding :generateProto tasks and are executed before corresponding
     * :compileJava tasks.
     */
    @Override
    void apply(Project project) {
        this.project = project

        final Task generateFailures = project.task("generateFailures") << {
            final GString path = getMainDescriptorSetPath(project)
            def filesWithFailures = getFailureProtoFileDescriptors(path)
            processDescriptors(filesWithFailures)
        }
        generateFailures.dependsOn("generateProto")
        project.tasks.getByPath("compileJava").dependsOn(generateFailures)

        final Task generateTestFailures = project.task("generateTestFailures") << {
            final GString path = getTestDescriptorSetPath(project)
            def filesWithFailures = getFailureProtoFileDescriptors(path)
            processDescriptors(filesWithFailures)
        }
        generateTestFailures.dependsOn("generateTestProto")
        project.tasks.getByPath("compileTestJava").dependsOn(generateTestFailures)
    }

    private void processDescriptors(List<FileDescriptorProto> descriptors) {
        descriptors.each { FileDescriptorProto file ->
            if (isFileWithFailures(file)) {
                generateFailures(file, cachedMessageTypes)
            } else {
                log.error("Invalid failures file: $file.name")
            }
        }
    }

    private List<FileDescriptorProto> getFailureProtoFileDescriptors(GString descFilePath) {
        final List<FileDescriptorProto> failureDescriptors = new LinkedList<>()
        final Collection<FileDescriptorProto> allDescriptors = getProtoFileDescriptors(descFilePath)
        for (FileDescriptorProto file : allDescriptors) {
            if (file.getName().endsWith("failures.proto")) {
                log.info("Found failures file: $file.name")
                failureDescriptors.add(file)
            }
            cacheTypes(file)
        }
        return failureDescriptors
    }

    private static boolean isFileWithFailures(FileDescriptorProto descriptor) {
        // By convention failures are generated into one file.
        if (descriptor.options.javaMultipleFiles) {
            return false
        }
        final GString javaOuterClassName = "$descriptor.options.javaOuterClassname"
        if (!javaOuterClassName) {
            // There's no outer class name given in options.
            // Assuming the file name ends with `failures.proto`, it's a good failures file.
            return true;
        }
        final boolean result = javaOuterClassName.endsWith("Failures")
        return result
    }

    private void cacheTypes(FileDescriptorProto fileDescriptor) {
        final GString protoPrefix = !fileDescriptor.package.isEmpty() ? "${fileDescriptor.package}." : GString.EMPTY
        GString javaPrefix = !fileDescriptor.options.javaPackage.isEmpty() ? "${fileDescriptor.options.javaPackage}." : GString.EMPTY
        if (!fileDescriptor.options.javaMultipleFiles) {
            final GString singleFileSuffix = getOuterClassName(fileDescriptor)
            javaPrefix = "${javaPrefix}${singleFileSuffix}."
        }
        fileDescriptor.messageTypeList.each { DescriptorProto msg ->
            cacheMessageType(msg, protoPrefix, javaPrefix)
        }
        fileDescriptor.enumTypeList.each { EnumDescriptorProto enumType ->
            cacheEnumType(enumType, protoPrefix, javaPrefix)
        }
    }

    private static GString getOuterClassName(FileDescriptorProto descriptor) {
        GString classname = "$descriptor.options.javaOuterClassname"
        if (!classname.isEmpty()) {
            return "$classname"
        }
        classname = "${descriptor.name.substring(descriptor.name.lastIndexOf('/') + 1, descriptor.name.lastIndexOf(".proto"))}"
        final GString result = "${classname.charAt(0).toUpperCase()}${classname.substring(1)}"
        return result
    }

    private void cacheEnumType(EnumDescriptorProto descriptor, GString protoPrefix, GString javaPrefix) {
        cachedMessageTypes.put("${protoPrefix}${descriptor.name}", "${javaPrefix}${descriptor.name}")
    }

    private void cacheMessageType(DescriptorProto msg, GString protoPrefix, GString javaPrefix) {
        cachedMessageTypes.put("${protoPrefix}${msg.name}", "${javaPrefix}${msg.name}")
        if (msg.nestedTypeCount > 0 || msg.enumTypeCount > 0) {
            final GString nestedProtoPrefix = "${protoPrefix}${msg.name}."
            final GString nestedJavaPrefix = "${javaPrefix}${msg.name}."
            for (DescriptorProto nestedMsg : msg.nestedTypeList) {
                cacheMessageType(nestedMsg, nestedProtoPrefix, nestedJavaPrefix)
            }
            for (EnumDescriptorProto enumType : msg.enumTypeList) {
                cacheEnumType(enumType, nestedProtoPrefix, nestedJavaPrefix)
            }
        }
    }

    private static String getJavaOuterClassName(FileDescriptorProto descriptor) {
        String outerClassNameFromOptions = descriptor.options.javaOuterClassname
        if (!outerClassNameFromOptions.isEmpty()) {
            return outerClassNameFromOptions;
        }

        final String fullFileName = descriptor.getName()
        int lastBackslash = fullFileName.lastIndexOf('/')
        final String onlyName = fullFileName.substring(lastBackslash + 1)
                                            .replace(".proto", "")
        return toCamelCase(onlyName)
    }

    private static String toCamelCase(String fileName) {
        final StringBuilder result = new StringBuilder(fileName.length());

        for (final String word : fileName.split("_")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                result.append(word.substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    private void generateFailures(FileDescriptorProto descriptor, Map<GString, GString> messageTypeMap) {
        final GString failuresRootDir = getTargetGenFailuresRootDir(project)
        final GString javaPackage = "$descriptor.options.javaPackage"
        final String javaOuterClassName = getJavaOuterClassName(descriptor)
        final GString packageDirs = "${javaPackage.replace(".", "/")}"
        final List<DescriptorProto> failures = descriptor.messageTypeList
        failures.each { DescriptorProto failure ->
            final GString failureJavaPath = "$failuresRootDir/$packageDirs/${failure.name}.java"
            final File outputFile = new File(failureJavaPath)
            new FailureWriter(failure, outputFile, javaPackage, javaOuterClassName, messageTypeMap).write()
        }
    }
}
