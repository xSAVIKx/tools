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

package org.spine3.gradle.failures

import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer

import static com.google.protobuf.DescriptorProtos.*
import static org.spine3.gradle.Extension.*
import static org.spine3.gradle.util.DescriptorSetUtil.getProtoFileDescriptors

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
            final String path = getMainDescriptorSetPath(project)
            processDescriptors(getFailureProtoFileDescriptors(path))
        }
        generateFailures.dependsOn("generateProto")

        final Task generateTestFailures = project.task("generateTestFailures") << {
            final String path = getTestDescriptorSetPath(project)
            processDescriptors(getFailureProtoFileDescriptors(path))
        }
        generateTestFailures.dependsOn("generateTestProto")

        final TaskContainer targetTasks = project.getTasks()
        targetTasks.getByPath("compileJava").dependsOn(generateFailures)
        targetTasks.getByPath("compileTestJava").dependsOn(generateTestFailures)
    }

    private void processDescriptors(List<FileDescriptorProto> descriptors) {
        descriptors.each { FileDescriptorProto file ->
            if (validateFailures(file)) {
                generateFailures(file, cachedMessageTypes)
            } else {
                log.error("Invalid failures file: $file.name")
            }
        }
    }

    private List<FileDescriptorProto> getFailureProtoFileDescriptors(String descFilePath) {
        final List<FileDescriptorProto> failureDescriptors = new LinkedList<>()
        final List<FileDescriptorProto> allDescriptors = getProtoFileDescriptors(descFilePath)
        for (FileDescriptorProto file : allDescriptors) {
            if (file.getName().endsWith("/failures.proto")) {
                failureDescriptors.add(file)
            }
            cacheTypes(file)
        }
        return failureDescriptors
    }

    private static boolean validateFailures(FileDescriptorProto descriptor) {
        final boolean javaMultipleFiles = descriptor.options.javaMultipleFiles
        final String javaOuterClassName = descriptor.options.javaOuterClassname
        final boolean result = !(javaMultipleFiles ||
                (javaOuterClassName && javaOuterClassName != "Failures"))
        return result
    }

    private void cacheTypes(FileDescriptorProto fileDescriptor) {
        final String protoPrefix = !fileDescriptor.package.isEmpty() ? "${fileDescriptor.package}." : ""
        String javaPrefix = !fileDescriptor.options.javaPackage.isEmpty() ? "${fileDescriptor.options.javaPackage}." : ""
        if (!fileDescriptor.options.javaMultipleFiles) {
            final String singleFileSuffix = getOuterClassName(fileDescriptor)
            javaPrefix = "${javaPrefix}${singleFileSuffix}."
        }
        fileDescriptor.messageTypeList.each { DescriptorProto msg ->
            cacheMessageType(msg, protoPrefix, javaPrefix)
        }
        fileDescriptor.enumTypeList.each { EnumDescriptorProto enumType ->
            cacheEnumType(enumType, protoPrefix, javaPrefix)
        }
    }

    private static String getOuterClassName(FileDescriptorProto descriptor) {
        String classname = descriptor.options.javaOuterClassname
        if (!classname.isEmpty()) {
            return classname
        }
        classname = descriptor.name.substring(descriptor.name.lastIndexOf('/') + 1, descriptor.name.lastIndexOf(".proto"))
        final String result = "${classname.charAt(0).toUpperCase()}${classname.substring(1)}"
        return result
    }

    private void cacheEnumType(EnumDescriptorProto descriptor, String protoPrefix, String javaPrefix) {
        cachedMessageTypes.put("${protoPrefix}${descriptor.name}", "${javaPrefix}${descriptor.name}")
    }

    private void cacheMessageType(DescriptorProto msg, String protoPrefix, String javaPrefix) {
        cachedMessageTypes.put("${protoPrefix}${msg.name}", "${javaPrefix}${msg.name}")
        if (msg.nestedTypeCount > 0 || msg.enumTypeCount > 0) {
            final String nestedProtoPrefix = "${protoPrefix}${msg.name}."
            final String nestedJavaPrefix = "${javaPrefix}${msg.name}."
            for (DescriptorProto nestedMsg : msg.nestedTypeList) {
                cacheMessageType(nestedMsg, nestedProtoPrefix, nestedJavaPrefix)
            }
            for (EnumDescriptorProto enumType : msg.enumTypeList) {
                cacheEnumType(enumType, nestedProtoPrefix, nestedJavaPrefix)
            }
        }
    }

    private void generateFailures(FileDescriptorProto descriptor, Map<GString, GString> messageTypeMap) {
        final String failuresRootDir = getTargetGenFailuresRootDir(project)
        final String packageDirs = descriptor.options.javaPackage.replace(".", "/")
        final List<DescriptorProto> failures = descriptor.messageTypeList
        failures.each { DescriptorProto failure ->
            final String failureJavaPath = "$failuresRootDir/$packageDirs/${failure.name}.java"
            final File outputFile = new File(failureJavaPath)
            new FailureWriter(failure, outputFile, descriptor.options.javaPackage, messageTypeMap).write()
        }
    }
}
