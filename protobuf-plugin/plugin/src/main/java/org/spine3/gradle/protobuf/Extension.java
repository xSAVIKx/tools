/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package org.spine3.gradle.protobuf;

import com.google.common.collect.ImmutableList;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static org.spine3.gradle.protobuf.ProtobufPlugin.SPINE_PROTOBUF_EXTENSION_NAME;

/**
 *
 * A configuration for the {@link ProtobufPlugin}.
 *
 * @author Alex Tymchenko
 */
@SuppressWarnings("PublicField")    // as this is a Gradle extension.
public class Extension {

    /**
     * The absolute path to the main target generated resources directory.
     */
    public String mainTargetGenResourcesDir;

    /**
     * The absolute path to the test target generated resources directory.
     */
    public String testTargetGenResourcesDir;

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
     * The absolute path to the test target generated failures root directory.
     */
    public String targetTestGenFailuresRootDir;

    /**
     * The absolute path to directory to delete.
     *
     * <p>Either this property OR {@code dirsToClean} property is used.
     */
    public String dirToClean;

    /**
     * The absolute paths to directories to delete.
     *
     * <p>Either this property OR {@code dirToClean} property is used.
     */
    public List<String> dirsToClean = new LinkedList<>();

    public static String getMainTargetGenResourcesDir(Project project) {
        final String path = spineProtobuf(project).mainTargetGenResourcesDir;
        if (isNullOrEmpty(path)) {
            return project.getProjectDir()
                          .getAbsolutePath() + "/generated/main/resources";
        } else {
            return path;
        }
    }

    public static String getTestTargetGenResourcesDir(Project project) {
        final String path = spineProtobuf(project).testTargetGenResourcesDir;
        if (isNullOrEmpty(path)) {
            return project.getProjectDir()
                          .getAbsolutePath() + "/generated/test/resources";
        } else {
            return path;
        }
    }

    public static String getMainDescriptorSetPath(Project project) {
        final String path = spineProtobuf(project).mainDescriptorSetPath;
        if (isNullOrEmpty(path)) {
            return project.getProjectDir()
                          .getAbsolutePath() + "/build/descriptors/main.desc";
        } else {
            return path;
        }
    }

    public static String getTestDescriptorSetPath(Project project) {
        final String path = spineProtobuf(project).testDescriptorSetPath;
        if (isNullOrEmpty(path)) {
            return project.getProjectDir()
                          .getAbsolutePath() + "/build/descriptors/test.desc";
        } else {
            return path;
        }
    }

    public static String getTargetGenFailuresRootDir(Project project) {
        final String path = spineProtobuf(project).targetGenFailuresRootDir;
        if (isNullOrEmpty(path)) {
            return project.getProjectDir()
                          .getAbsolutePath() + "/generated/main/spine";
        } else {
            return path;
        }
    }

    public static String getTargetTestGenFailuresRootDir(Project project) {
        final String path = spineProtobuf(project).targetTestGenFailuresRootDir;
        if (isNullOrEmpty(path)) {
            return project.getProjectDir()
                          .getAbsolutePath() + "/generated/test/spine";
        } else {
            return path;
        }
    }

    public static List<String> getDirsToClean(Project project) {
        log().debug("Finding the directories to clean");
        final List<String> dirs = spineProtobuf(project).dirsToClean;
        if (dirs.size() > 0) {
            log().error("Found {} directories to clean: {}", dirs.size(), dirs);
            return ImmutableList.copyOf(dirs);
        }
        final String singleDir = spineProtobuf(project).dirToClean;
        if (singleDir != null && !singleDir.isEmpty()) {
            log().debug("Found directory to clean: {}", singleDir);
            return singletonList(singleDir);
        }
        final String defaultValue = project.getProjectDir()
                                           .getAbsolutePath() + "/generated";
        log().debug("Default directory to clean: {}", defaultValue);
        return singletonList(defaultValue);
    }

    private static Extension spineProtobuf(Project project) {
        return (Extension) project.getExtensions()
                                  .getByName(SPINE_PROTOBUF_EXTENSION_NAME);
    }

    private static Logger log() {
        return LoggerSingleton.INSTANCE.logger;
    }

    private enum LoggerSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger logger = LoggerFactory.getLogger(Extension.class);
    }
}
