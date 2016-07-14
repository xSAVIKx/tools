/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spine3.gradle

import com.google.common.collect.ImmutableList
import org.gradle.api.Project

import static java.util.Collections.singletonList

/**
 * A config for the {@link ProtobufPlugin}.
 */
class Extension {

    /**
     * The absolute path to the main target generated resources directory.
     */
    String mainTargetGenResourcesDir

    /**
     * The absolute path to the test target generated resources directory.
     */
    String testTargetGenResourcesDir

    /**
     * The absolute path to the main Protobuf sources directory.
     */
    String mainProtoSrcDir

    /**
     * The absolute path to the test Protobuf sources directory.
     */
    String testProtoSrcDir

    /**
     * The absolute path to the main Protobuf descriptor set file.
     */
    String mainDescriptorSetPath

    /**
     * The absolute path to the test Protobuf descriptor set file.
     */
    String testDescriptorSetPath

    /**
     * The absolute path to the main target generated failures root directory.
     */
    String targetGenFailuresRootDir

    /**
     * The absolute path to directory to delete.
     *
     * <p>Either this property OR {@code dirsToClean} property is used.
     */
    String dirToClean

    /**
     * The absolute paths to directories to delete.
     *
     * <p>Either this property OR {@code dirToClean} property is used.
     */
    String[] dirsToClean = []

    public static String getMainTargetGenResourcesDir(Project project) {
        final String path = project.spineProtobuf.mainTargetGenResourcesDir
        if (path == null) {
            return "$project.projectDir.absolutePath/generated/main/resources"
        } else {
            return path
        }
    }

    public static String getTestTargetGenResourcesDir(Project project) {
        final String path = project.spineProtobuf.testTargetGenResourcesDir
        if (path == null) {
            return "$project.projectDir.absolutePath/generated/test/resources"
        } else {
            return path
        }
    }

    public static String getMainProtoSrcDir(Project project) {
        final String path = project.spineProtobuf.mainProtoSrcDir
        if (path == null) {
            return "$project.projectDir.absolutePath/src/main/proto"
        } else {
            return path
        }
    }

    public static String getTestProtoSrcDir(Project project) {
        final String path = project.spineProtobuf.testProtoSrcDir
        if (path == null) {
            return "$project.projectDir.absolutePath/src/test/proto"
        } else {
            return path
        }
    }

    public static String getMainDescriptorSetPath(Project project) {
        final String path = project.spineProtobuf.mainDescriptorSetPath
        if (path == null) {
            return "$project.projectDir.absolutePath/build/descriptors/main.desc"
        } else {
            return path
        }
    }

    public static String getTestDescriptorSetPath(Project project) {
        final String path = project.spineProtobuf.testDescriptorSetPath
        if (path == null) {
            return "$project.projectDir.absolutePath/build/descriptors/test.desc"
        } else {
            return path
        }
    }

    public static String getTargetGenFailuresRootDir(Project project) {
        final String path = project.spineProtobuf.targetGenFailuresRootDir
        if (path == null) {
            return "$project.projectDir.absolutePath/generated/main/spine"
        } else {
            return path
        }
    }

    public static List<String> getDirsToClean(Project project) {
        final String[] dirs = project.spineProtobuf.dirsToClean
        if (dirs.length > 0) {
            return ImmutableList.copyOf(dirs)
        }
        final String singleDir = project.spineProtobuf.dirToClean
        if (singleDir != null) {
            return singletonList(singleDir)
        }
        return singletonList("$project.projectDir.absolutePath/generated")
    }
}
