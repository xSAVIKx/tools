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
import org.spine3.gradle.protobuf.util.JavaCode

import static com.google.protobuf.DescriptorProtos.*
import static org.spine3.gradle.protobuf.Extension.*
import static org.spine3.gradle.protobuf.util.DescriptorSetUtil.getProtoFileDescriptors
/**
 * Plugin which generates Failures, based on failures.proto files.
 *
 * <p>Uses generated proto descriptors.
 *
 * <p>Logs a warning if there are no protobuf descriptors generated.
 */
@Slf4j
class FailuresGenPlugin implements Plugin<Project> {

    private Project project

    /** A map from Protobuf type name to Java class FQN. */
    private Map<GString, GString> cachedMessageTypes = new HashMap<>()

    /**
     * Applies the plug-in to a project.
     *
     * <p>Adds {@code :generateFailures} and {@code :generateTestFailures} tasks.
     *
     * <p>Tasks depend on corresponding {@code :generateProto} tasks and are executed before corresponding
     * {@code :compileJava} tasks.
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

    private List<FileDescriptorProto> getFailureProtoFileDescriptors(GString descFilePath) {
        final List<FileDescriptorProto> result = new LinkedList<>()
        final Collection<FileDescriptorProto> allDescriptors = getProtoFileDescriptors(descFilePath)
        for (FileDescriptorProto file : allDescriptors) {
            if (file.getName().endsWith("failures.proto")) {
                log.info("Found failures file: $file.name")
                result.add(file)
            }
            cacheTypes(file)
        }
        return result
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

    //TODO:2016-10-07:alexander.yevsyukov: Move type loading routines into a separate class.
    private void cacheTypes(FileDescriptorProto fileDescriptor) {
        final GString protoPackage = !fileDescriptor.package.isEmpty() ? "${fileDescriptor.package}." : GString.EMPTY
        GString javaPackage = !fileDescriptor.options.javaPackage.isEmpty() ? "${fileDescriptor.options.javaPackage}." : GString.EMPTY
        if (!fileDescriptor.options.javaMultipleFiles) {
            final GString singleFileSuffix = JavaCode.getOuterClassName(fileDescriptor)
            javaPackage = "${javaPackage}${singleFileSuffix}."
        }
        fileDescriptor.messageTypeList.each { DescriptorProto msg ->
            cacheMessageType(msg, protoPackage, javaPackage)
        }
        fileDescriptor.enumTypeList.each { EnumDescriptorProto enumType ->
            cacheEnumType(enumType, protoPackage, javaPackage)
        }
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

    private void generateFailures(FileDescriptorProto descriptor, Map<GString, GString> messageTypeMap) {
        final GString failuresRootDir = getTargetGenFailuresRootDir(project)
        final GString javaPackage = "$descriptor.options.javaPackage"
        final String javaOuterClassName = JavaCode.getJavaOuterClassName(descriptor)
        final GString packageDir = "${javaPackage.replace(".", "/")}"
        final List<DescriptorProto> failures = descriptor.messageTypeList
        failures.each { DescriptorProto failure ->
            // The name of the generated ThrowableFailure will be the same as for the Protobuf message.
            final GString failureJavaPath = "$failuresRootDir/$packageDir/${failure.name}.java"
            final File outputFile = new File(failureJavaPath)
            final FailureWriter writer = new FailureWriter(failure, outputFile, javaPackage, javaOuterClassName, messageTypeMap)
            writer.write()
        }
    }
}
