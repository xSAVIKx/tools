package org.spine3.gradle.cleaning
import com.google.common.collect.ImmutableList
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import static org.spine3.gradle.ProtobufPlugin.getDirsToClean

/**
 * Plugin which performs additional cleaning on clean task.
 */
class CleaningPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        final Task preClean = project.task("preClean") << {
            deleteDirs(getDirsToClean(project));
        }
        final def tasks = project.getTasks()
        tasks.getByPath("clean").dependsOn(preClean);
    }

    private static void deleteDirs(String[] dirs) {
        for (String path : ImmutableList.copyOf(dirs)) {
            final def file = new File(path);
            if (file.exists() && file.isDirectory()) {
                file.deleteDir();
            }
        }
    }
}
