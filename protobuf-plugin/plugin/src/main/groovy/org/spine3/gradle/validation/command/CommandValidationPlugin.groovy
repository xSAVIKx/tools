package org.spine3.gradle.validation.command

import com.google.protobuf.Descriptors
import com.google.protobuf.MessageOrBuilder;
import groovy.util.logging.Slf4j;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.spine3.gradle.utils.FileUtil;
import org.spine3.gradle.utils.Paths;
import org.spine3.gradle.utils.ProtoUtil;

@Slf4j
class CommandValidationPlugin implements Plugin<Project> {

    private static final String COMMANDS_OUTER_JAVA_NAME_SUFFIX = "CommandsProto";

    private static final String COMMANDS_FILE_NAME = "commands.proto";

    private String projectPath;

    @Override
    void apply(Project target) {

        projectPath = target.projectDir.absolutePath;

        final Task generateCommandValidators = target.task("generateCommandValidators") {
            generateCommandValidators(target, extendClassPath(projectPath));
        }

        generateCommandValidators.dependsOn("compileJava", "generateProto");
        target.getTasks().getByPath("processResources").dependsOn(generateCommandValidators);
    }

    private static ClassLoader extendClassPath(String projectPath) {
        final String javaClassesRoot = "$projectPath${Paths.CLASSES_DIR_RELATIVE_PATH}";

        URL fileUrl = new File(javaClassesRoot).toURI().toURL();

        URL[] args = [fileUrl] as URL[];

        URLClassLoader classLoader = new URLClassLoader(args, this.classLoader);

        return classLoader;
    }

    private void generateCommandValidators(Project target, ClassLoader classLoader) {

        final List<File> commandsFiles = FileUtil.findAllFiles(target,
                "$projectPath${Paths.PROTO_DIR_RELATIVE_PATH}", COMMANDS_FILE_NAME);

        final Map<String, Descriptors.FileDescriptor> commandsDescriptors = new HashMap<>();

        for (File commandsFile : commandsFiles) {
            final ProtoUtil.ProtoFileMetadata protoFileMetadata = ProtoUtil.readProtoMetadata(commandsFile);

            final Class messageClass = classLoader.loadClass("${protoFileMetadata.javaPackage}" +
                    ".${protoFileMetadata.firstFoundMessageName}");

            if (!MessageOrBuilder.class.isAssignableFrom(messageClass)) {
                continue;
            }

            def descriptor = ProtoUtil.getClassDescriptor(messageClass).file;

            if (!commandsDescriptors.containsKey(descriptor.fullName)) {
                commandsDescriptors.put(descriptor.fullName, descriptor);
            }
        }

        for (Descriptors.FileDescriptor file : commandsDescriptors.values()) {
            processCommandsFile(target, file);
        }
    }

    private void processCommandsFile(Project target, Descriptors.FileDescriptor file) {
        // decide how to name file
        // -- maybe Validator and the same package?
        // for each command read fields
        // if the field contains [required] option, generate validator

        final String javaPackage = file.options.javaPackage;

        String aggregateName = null;

        final String outerClassName = file.options.javaOuterClassname;
        if (outerClassName.endsWith(COMMANDS_OUTER_JAVA_NAME_SUFFIX)) {
            aggregateName = outerClassName.substring(0, outerClassName.size() -
                    COMMANDS_OUTER_JAVA_NAME_SUFFIX.size());
        } else {
            // TODO:2016-02-05:mikhail.mikhaylov: Handle empty java package case.
            aggregateName = javaPackage.substring(javaPackage.lastIndexOf('.') + 1);
        }

        aggregateName = "${aggregateName.charAt(0).toUpperCase()}${aggregateName.substring(1).toLowerCase()}";

        final String validatorFilePath = "$projectPath${Paths.SPINE_GENERATED_JAVA_RELATIVE_PATH}" +
                "/${javaPackage.replace(".", "/")}/${aggregateName}Validator.java";

        final File validatorFile = new File(validatorFilePath);
        validatorFile.parentFile.mkdirs();
        validatorFile.createNewFile();

    }
}
