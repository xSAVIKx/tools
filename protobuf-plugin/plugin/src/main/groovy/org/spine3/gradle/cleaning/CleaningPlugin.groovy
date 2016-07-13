package org.spine3.gradle.cleaning

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import static org.spine3.gradle.ProtobufPlugin.getDirsToClean

/**
 * Plugin which performs additional cleaning on clean task.
 */
class CleaningPlugin implements Plugin<Project> {

    /**
     * Adds `:preClean` task, whic is executed before `:clean` task.
     */
    @Override
    void apply(Project project) {
        final Task preClean = project.task("preClean") << {
            deleteDirs(getDirsToClean(project));
        }
        final def tasks = project.getTasks()
        tasks.getByPath("clean").dependsOn(preClean);
    }

    private static void deleteDirs(List<String> dirs) {
        for (String dirPath : dirs) {
            final def file = new File(dirPath);
            if (file.exists() && file.isDirectory()) {
                file.deleteDir();
            }
        }
    }
}
