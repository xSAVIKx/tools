package org.spine3.gradle.cleaning

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Plugin which performs additional cleaning on clean task.
 *
 * <p>Currently removes generated files directory. To clean additional directories,
 * we need to extend this plugin's functionality with adding configurable closure
 * or add these directories to {@code defaultDirsToClean}.
 */
class CleaningPlugin implements Plugin<Project> {

    private String projectPath;

    private def defaultDirsToClean = ["generated"];

    @Override
    void apply(Project target) {
        projectPath = target.projectDir.absolutePath;

        final Task preClean = target.task("preClean") << {
            def dirsToClean = collectDirs();
            cleanDirs(dirsToClean);
        }

        final def targetTasks = target.getTasks()
        targetTasks.getByPath("clean").dependsOn(preClean);
    }

    private List<String> collectDirs() {

        final def dirs = new ArrayList<String>();

        for (String dir : defaultDirsToClean) {
            dirs.add(getAbsolutePath(dir));
        }

        return dirs;
    }

    private static void cleanDirs(List<String> paths) {
        for (String path : paths) {
            final def file = new File(path);
            if (file.exists() && file.isDirectory()) {
                file.deleteDir();
            }
        }
    }

    private String getAbsolutePath(String dirRelativePath) {
        return projectPath + "/" + dirRelativePath;
    }
}
