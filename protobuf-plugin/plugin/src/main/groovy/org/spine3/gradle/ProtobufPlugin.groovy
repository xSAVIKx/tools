package org.spine3.gradle
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.spine3.gradle.cleaning.CleaningPlugin
import org.spine3.gradle.failures.FailuresGenPlugin
import org.spine3.gradle.lookup.enrichments.EnrichmentLookupPlugin
import org.spine3.gradle.lookup.proto.ProtoToJavaMapperPlugin
/**
 * Root plugin, which aggregates other plugins.
 */
class ProtobufPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create("spineProtobuf", Extension.class);
        new CleaningPlugin().apply(project);
        new ProtoToJavaMapperPlugin().apply(project);
        new EnrichmentLookupPlugin().apply(project);
        new FailuresGenPlugin().apply(project);
    }

    public static String getMainTargetGenResourcesDir(Project project) {
        final String path = project.spineProtobuf.mainTargetGenResourcesDir;
        if (path == null) {
            return "$project.projectDir.absolutePath/generated/main/resources";
        } else {
            return path;
        }
    }

    public static String getTestTargetGenResourcesDir(Project project) {
        final String path = project.spineProtobuf.testTargetGenResourcesDir;
        if (path == null) {
            return "$project.projectDir.absolutePath/generated/test/resources";
        } else {
            return path;
        }
    }

    public static String getMainProtoSrcDir(Project project) {
        def path = project.spineProtobuf.mainProtoSrcDir;
        if (path == null) {
            return "$project.projectDir.absolutePath/src/main/proto";
        } else {
            return path;
        }
    }

    public static String getTestProtoSrcDir(Project project) {
        final String path = project.spineProtobuf.testProtoSrcDir;
        if (path == null) {
            return "$project.projectDir.absolutePath/src/test/proto";
        } else {
            return path;
        }
    }

    public static String getMainDescriptorSetPath(Project project) {
        final String path = project.spineProtobuf.mainDescriptorSetPath;
        if (path == null) {
            return "$project.projectDir.absolutePath/build/descriptors/main.desc";
        } else {
            return path;
        }
    }

    public static String getTestDescriptorSetPath(Project project) {
        final String path = project.spineProtobuf.testDescriptorSetPath;
        if (path == null) {
            return "$project.projectDir.absolutePath/build/descriptors/test.desc";
        } else {
            return path;
        }
    }

    public static String getTargetGenFailuresRootDir(Project project) {
        final String path = project.spineProtobuf.targetGenFailuresRootDir;
        if (path == null) {
            return "$project.projectDir.absolutePath/generated/main/spine";
        } else {
            return path;
        }
    }

    public static String[] getDirsToClean(Project project) {
        final String[] dirs = project.spineProtobuf.dirsToClean;
        if (dirs.length == 0) {
            return ["$project.projectDir.absolutePath/generated"];
        } else {
            return dirs;
        }
    }

    /**
     * A config for the plugin.
     */
    public static class Extension {

        /**
         * The absolute path to the main target generated resources directory.
         */
        public String mainTargetGenResourcesDir;

        /**
         * The absolute path to the test target generated resources directory.
         */
        public String testTargetGenResourcesDir;

        /**
         * The absolute path to the main Protobuf sources directory.
         */
        public String mainProtoSrcDir;

        /**
         * The absolute path to the test Protobuf sources directory.
         */
        public String testProtoSrcDir;

        /**
         * The absolute path to the main Protobuf descriptor set file.
         */
        public String mainDescriptorSetPath;

        /**
         * The absolute path to the test Protobuf descriptor set file.
         */
        public String testDescriptorSetPath;

        /**
         * The absolute path to the main target generated failures root directory.
         */
        public String targetGenFailuresRootDir;

        /**
         * The absolute paths to directories to delete.
         */
        public String[] dirsToClean = [];
    }
}
