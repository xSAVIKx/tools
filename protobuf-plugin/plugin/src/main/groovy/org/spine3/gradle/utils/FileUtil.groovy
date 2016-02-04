package org.spine3.gradle.utils

import org.gradle.api.Nullable
import org.gradle.api.Project

@SuppressWarnings("UtilityClass")
class FileUtil {

    private FileUtil() {}

    /**
     * Searches for a file with given name under the given path.
     *
     * @param target Project which contains file
     * @param rootPath root search path
     * @param fileName file's simple name without path
     * @return found file, null if nothing's found
     */
    @Nullable
    public static File findFile(Project target, String rootPath, String fileName) {
        File result = null;

        File root = new File(rootPath);
        target.fileTree(root).each {
            if (it.name.equals(fileName)) {
                result = it;
            }
        }

        return result;
    }
}
