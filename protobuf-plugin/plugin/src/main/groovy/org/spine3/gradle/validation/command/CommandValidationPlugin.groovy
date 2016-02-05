package org.spine3.gradle.validation.command;

import groovy.util.logging.Slf4j;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task
import org.spine3.gradle.utils.FileUtil
import org.spine3.gradle.utils.Paths
import org.spine3.gradle.utils.ProtoUtil;

@Slf4j
class CommandValidationPlugin implements Plugin<Project> {

    private static final String COMMANDS_FILE_NAME = "commands.proto";

    private String projectPath;

    @Override
    void apply(Project target) {

        projectPath = target.projectDir.absolutePath;

        final Task generateCommandValidators = target.task("generateCommandValidators") {
            generateCommandValidators(target);
        }

        generateCommandValidators.dependsOn("compileJava", "generateProto");
        target.getTasks().getByPath("processResources").dependsOn(generateCommandValidators);
    }

    private void generateCommandValidators(Project target) {

        final List<File> commandsFiles = FileUtil.findAllFiles(target,
                "$projectPath${Paths.PROTO_DIR_RELATIVE_PATH}", COMMANDS_FILE_NAME);

        for (File commandsFile : commandsFiles) {
            final String javaPackage = ProtoUtil.readJavaPackageFromProto(commandsFile);
            // scan the whole package
            // load the whole package into classpath. For each read class try reading descriptor
            // have a set of descriptors
            // each descriptor is a commants.proto file. For each of them we should perform
            // -- similar action
        }
    }
}
