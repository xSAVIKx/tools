package org.spine3.gradle.validation.command;

import groovy.util.logging.Slf4j;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

@Slf4j
class CommandValidationPlugin implements Plugin<Project> {

    private String projectPath;

    @Override
    void apply(Project target) {

        projectPath = target.projectDir.absolutePath;

        final Task generateCommandValidators = target.task("generateCommandValidators") {

        }

        generateCommandValidators.dependsOn("compileJava", "generateProto");
        target.getTasks().getByPath("processResources").dependsOn(generateCommandValidators);
    }
}
