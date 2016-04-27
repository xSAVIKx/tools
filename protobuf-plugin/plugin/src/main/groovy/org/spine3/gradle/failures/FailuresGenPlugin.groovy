package org.spine3.gradle.failures

import com.google.protobuf.DescriptorProtos
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Plugin, which generates Failures, based on failures.proto files.
 *
 * <p>Uses generated proto descriptors.
 * <p>Generates warning if there are no protobuf descriptors generated.
 */
@Slf4j
class FailuresGenPlugin implements Plugin<Project> {

    private String projectPath;

    private Map<String, String> cachedMessageTypes = new HashMap<>();

    @Override
    void apply(Project target) {
        projectPath = target.projectDir.absolutePath;

        final Task generateFailures = target.task("generateFailures") << {

            def descriptors = readFailureDescriptors(target);

            descriptors.each { def descriptor ->
                if (validateFailures(descriptor)) {
                    generateFailures(descriptor, cachedMessageTypes);
                } else {
                    log.error("Invalid failures file");
                }
            }
        };

        generateFailures.dependsOn("generateProto");
        target.getTasks().getByPath("processResources").dependsOn(generateFailures);
    }

    private List<DescriptorProtos.FileDescriptorProto> readFailureDescriptors(Project target) {
        final List<DescriptorProtos.FileDescriptorProto> failureDescriptors = new ArrayList<>();
        final String filePath = "${target.projectDir.absolutePath}/build/descriptors/main.desc";

        if (!new File(filePath).exists()) {
            log.warn("Please enable descriptor set generation. See an appropriate section at https://github.com/google/protobuf-gradle-plugin/blob/master/README.md#customize-code-generation-tasks")
            return new ArrayList<DescriptorProtos.FileDescriptorProto>();
        }

        new FileInputStream(filePath).withStream {
            final DescriptorProtos.FileDescriptorSet descriptorSet = DescriptorProtos.FileDescriptorSet
                    .parseFrom(it);

            descriptorSet.fileList.each { DescriptorProtos.FileDescriptorProto descriptor ->
                if (descriptor.getName().endsWith("/failures.proto")) {
                    failureDescriptors.add(descriptor);
                }
                cacheFieldTypes(descriptor);
            }
        }

        return failureDescriptors;
    }

    private static boolean validateFailures(DescriptorProtos.FileDescriptorProto descriptor) {
        final def javaMultipleFiles = descriptor.options.javaMultipleFiles
        final def javaOuterClassName = descriptor.options.javaOuterClassname
        final def javaOuterClassNameNotEmpty = javaOuterClassName != null && !javaOuterClassName.isEmpty()
        return !(javaMultipleFiles || (javaOuterClassNameNotEmpty && !javaOuterClassName.equals("Failures")));
    }

    private void cacheFieldTypes(DescriptorProtos.FileDescriptorProto fileDescriptor) {
        def protoPrefix = !fileDescriptor.package.isEmpty() ? "${fileDescriptor.package}." : "";
        def javaPrefix = !fileDescriptor.options.javaPackage.isEmpty() ? "${fileDescriptor.options.javaPackage}." : "";
        if (!fileDescriptor.options.javaMultipleFiles) {
            def singleFileSuffix = getOuterClassName(fileDescriptor);
            javaPrefix = "${javaPrefix}${singleFileSuffix}."
        }
        fileDescriptor.messageTypeList.each { def field ->
            cacheFieldType(field, protoPrefix, javaPrefix);
        }
        fileDescriptor.enumTypeList.each { def enumType ->
            cacheEnumType(enumType, protoPrefix, javaPrefix);
        }
    }

    private static String getOuterClassName(DescriptorProtos.FileDescriptorProto descriptor) {
        def classname = descriptor.options.javaOuterClassname;
        if (!classname.isEmpty()) {
            return classname;
        }
        classname = descriptor.name.substring(descriptor.name.lastIndexOf('/') + 1, descriptor.name.lastIndexOf(".proto"));
        return "${classname.charAt(0).toUpperCase()}${classname.substring(1)}";
    }

    private void cacheEnumType(DescriptorProtos.EnumDescriptorProto descriptor, String protoPrefix, String javaPrefix) {
        cachedMessageTypes.put("${protoPrefix}${descriptor.name}", "${javaPrefix}${descriptor.name}");
    }

    private void cacheFieldType(DescriptorProtos.DescriptorProto descriptor, String protoPrefix, String javaPrefix) {
        cachedMessageTypes.put("${protoPrefix}${descriptor.name}", "${javaPrefix}${descriptor.name}");
        if (descriptor.nestedTypeCount > 0 || descriptor.enumTypeCount > 0) {
            def nestedProtoPrefix = "${protoPrefix}${descriptor.name}.";
            def nestedJavaPrefix = "${javaPrefix}${descriptor.name}.";
            for (def nestedDescriptor : descriptor.nestedTypeList) {
                cacheFieldType(nestedDescriptor, nestedProtoPrefix, nestedJavaPrefix);
            }
            for (def enumType : descriptor.enumTypeList) {
                cacheEnumType(enumType, nestedProtoPrefix, nestedJavaPrefix);
            }
        }
    }

    private void generateFailures(DescriptorProtos.FileDescriptorProto descriptor, Map<String, String> messageTypeMap) {
        String failuresFolderPath = projectPath + "/generated/main/spine/" + descriptor.options.javaPackage.replace(".", "/");

        final List<DescriptorProtos.DescriptorProto> failures = descriptor.messageTypeList;
        failures.each { DescriptorProtos.DescriptorProto failure ->
            File outputFile = new File(failuresFolderPath + "/" + failure.name + ".java");
            writeFailureIntoFile(failure, outputFile, descriptor.options.javaPackage, messageTypeMap);
        }
    }

    private static void writeFailureIntoFile(DescriptorProtos.DescriptorProto failure, File file, String javaPackage,
                                             Map<String, String> messageTypeMap) {
        new FailureWriter(failure, file, javaPackage, messageTypeMap).write();
    }
}
