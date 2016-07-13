package org.spine3.gradle.failures
import com.google.protobuf.DescriptorProtos
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import static com.google.protobuf.DescriptorProtos.FileDescriptorProto
import static org.spine3.gradle.ProtobufPlugin.*
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

    private Map<String, String> cachedMessageTypes = new HashMap<>()

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
            final def path = ExtensionUtil.getMainDescriptorSetPath(project)
            processDescriptors(getFailureProtoFileDescriptors(path))
        }
        generateFailures.dependsOn("generateProto")

        final Task generateTestFailures = project.task("generateTestFailures") << {
            final def path = ExtensionUtil.getTestDescriptorSetPath(project)
            processDescriptors(getFailureProtoFileDescriptors(path))
        }
        generateTestFailures.dependsOn("generateTestProto")

        final def targetTasks = project.getTasks()
        targetTasks.getByPath("compileJava").dependsOn(generateFailures)
        targetTasks.getByPath("compileTestJava").dependsOn(generateTestFailures)
    }

    private void processDescriptors(List<FileDescriptorProto> descriptors) {
        descriptors.each { def descriptor ->
            if (validateFailures(descriptor)) {
                generateFailures(descriptor, cachedMessageTypes)
            } else {
                log.error("Invalid failures file")
            }
        }
    }

    private List<FileDescriptorProto> getFailureProtoFileDescriptors(String descFilePath) {
        final List<FileDescriptorProto> failureDescriptors = new LinkedList<>()
        def allDescriptors = getProtoFileDescriptors(descFilePath)
        for (FileDescriptorProto file : allDescriptors) {
            if (file.getName().endsWith("/failures.proto")) {
                failureDescriptors.add(file)
            }
            cacheFieldTypes(file)
        }
        return failureDescriptors
    }

    private static boolean validateFailures(FileDescriptorProto descriptor) {
        final def javaMultipleFiles = descriptor.options.javaMultipleFiles
        final def javaOuterClassName = descriptor.options.javaOuterClassname
        final def javaOuterClassNameNotEmpty = javaOuterClassName != null && !javaOuterClassName.isEmpty()
        final def result =
                !(javaMultipleFiles ||
                 (javaOuterClassNameNotEmpty && !javaOuterClassName.equals("Failures")))
        return result
    }

    private void cacheFieldTypes(FileDescriptorProto fileDescriptor) {
        def protoPrefix = !fileDescriptor.package.isEmpty() ? "${fileDescriptor.package}." : ""
        def javaPrefix = !fileDescriptor.options.javaPackage.isEmpty() ? "${fileDescriptor.options.javaPackage}." : ""
        if (!fileDescriptor.options.javaMultipleFiles) {
            def singleFileSuffix = getOuterClassName(fileDescriptor)
            javaPrefix = "${javaPrefix}${singleFileSuffix}."
        }
        fileDescriptor.messageTypeList.each { def field ->
            cacheFieldType(field, protoPrefix, javaPrefix)
        }
        fileDescriptor.enumTypeList.each { def enumType ->
            cacheEnumType(enumType, protoPrefix, javaPrefix)
        }
    }

    private static String getOuterClassName(FileDescriptorProto descriptor) {
        def classname = descriptor.options.javaOuterClassname
        if (!classname.isEmpty()) {
            return classname
        }
        classname = descriptor.name.substring(descriptor.name.lastIndexOf('/') + 1, descriptor.name.lastIndexOf(".proto"))
        final def result = "${classname.charAt(0).toUpperCase()}${classname.substring(1)}"
        return result
    }

    private void cacheEnumType(DescriptorProtos.EnumDescriptorProto descriptor, String protoPrefix, String javaPrefix) {
        cachedMessageTypes.put("${protoPrefix}${descriptor.name}", "${javaPrefix}${descriptor.name}")
    }

    private void cacheFieldType(DescriptorProtos.DescriptorProto descriptor, String protoPrefix, String javaPrefix) {
        cachedMessageTypes.put("${protoPrefix}${descriptor.name}", "${javaPrefix}${descriptor.name}")
        if (descriptor.nestedTypeCount > 0 || descriptor.enumTypeCount > 0) {
            final def nestedProtoPrefix = "${protoPrefix}${descriptor.name}."
            final def nestedJavaPrefix = "${javaPrefix}${descriptor.name}."
            for (def nestedDescriptor : descriptor.nestedTypeList) {
                cacheFieldType(nestedDescriptor, nestedProtoPrefix, nestedJavaPrefix)
            }
            for (def enumType : descriptor.enumTypeList) {
                cacheEnumType(enumType, nestedProtoPrefix, nestedJavaPrefix)
            }
        }
    }

    private void generateFailures(FileDescriptorProto descriptor, Map<String, String> messageTypeMap) {
        final String failuresRootDir = ExtensionUtil.getTargetGenFailuresRootDir(project)
        final String packageDirs = descriptor.options.javaPackage.replace(".", "/")
        final List<DescriptorProtos.DescriptorProto> failures = descriptor.messageTypeList
        failures.each { DescriptorProtos.DescriptorProto failure ->
            final String failureJavaPath = "$failuresRootDir/$packageDirs/${failure.name}.java"
            final File outputFile = new File(failureJavaPath)
            writeFailureIntoFile(failure, outputFile, descriptor.options.javaPackage, messageTypeMap)
        }
    }

    private static void writeFailureIntoFile(DescriptorProtos.DescriptorProto failure, File file, String javaPackage,
                                             Map<String, String> messageTypeMap) {
        new FailureWriter(failure, file, javaPackage, messageTypeMap).write()
    }
}
