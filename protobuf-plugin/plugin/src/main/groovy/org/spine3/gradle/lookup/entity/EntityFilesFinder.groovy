/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.gradle.lookup.entity

import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.file.FileTree

import static ProtoParser.ProtoFileMetadata

/**
 * Finds {@code .proto} files located in packages belonging to an entity (containing entity state, commands, etc)
 * and provides the files metadata.
 *
 * @author Alexander Litus
 */
@Slf4j
public class EntityFilesFinder {

    private static final String PROTO_FILE_MASK = "**/*.proto";

    private final Project project;
    private final String searchDirPath;

    /**
     * Creates a new instance.
     *
     * @param project a target project
     * @param searchDirPath an absolute path to the directory to search in
     */
    public EntityFilesFinder(Project project, String searchDirPath) {
        this.project = project;
        this.searchDirPath = searchDirPath;
    }

    /**
     * Finds {@code .proto} files located in packages belonging to an entity and provides the files metadata.
     *
     * @return a set of files metadata
     */
    public Collection<ProtoFileMetadata> findFiles() {
        // Do not name this method "find" to avoid a confusion with "DefaultGroovyMethods.find()".
        final List<ProtoFileMetadata> result = new LinkedList<>();
        final Set<File> files = getProtoFiles();
        for (File file : files) {
            final ProtoFileMetadata data = ProtoParser.parse(file);
            if (data.isEntityStateDefined()) {
                if (!data.getProtoPackage().isEmpty()) {
                    result.add(data);
                } else {
                    log.error("Protobuf package must be set in the file defining entity state: ${data.getFileName()}");
                }
            }
        }
        return result;
    }

    private Set<File> getProtoFiles() {
        final File root = new File(searchDirPath);
        final FileTree fileTree = project.fileTree(root);
        fileTree.include(PROTO_FILE_MASK);
        final Set<File> files = fileTree.getFiles();
        return files;
    }
}
