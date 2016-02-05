package org.spine3.gradle.utils

import org.gradle.api.Nullable
import org.gradle.api.Project

@SuppressWarnings("UtilityClass")
class FileUtil {

    private FileUtil() {}

    /**
     * Searches for a file with given name under the given path.
     *
     * When this method is called, we assume, that there should be only one existing file, so first found is ok.
     *
     * @param target Project which contains file
     * @param rootPath root search path
     * @param fileName file's simple name without path
     * @return found file, null if nothing's found
     */
    @Nullable
    public static File findFile(Project target, String rootPath, String fileName) {
        def foundFiles = findAllFiles(target, rootPath, fileName)
        return foundFiles.size() > 0 ? foundFiles.get(0) : null;
    }

    public static List<File> findAllFiles(Project target, String rootPath, String fileName) {
        final List<File> searchResult = new ArrayList<>();

        final File root = new File(rootPath);
        target.fileTree(root).each {
            if (it.name.equals(fileName)) {
                searchResult.add(it);
            }
        }

        return searchResult;
    }
}
