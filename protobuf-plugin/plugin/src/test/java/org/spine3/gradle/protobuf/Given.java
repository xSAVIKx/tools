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

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;

import java.util.UUID;

import static org.spine3.gradle.TaskName.CLEAN;
import static org.spine3.gradle.TaskName.COMPILE_JAVA;
import static org.spine3.gradle.TaskName.COMPILE_TEST_JAVA;
import static org.spine3.gradle.TaskName.GENERATE_PROTO;
import static org.spine3.gradle.TaskName.GENERATE_TEST_PROTO;
import static org.spine3.gradle.TaskName.PROCESS_RESOURCES;
import static org.spine3.gradle.TaskName.PROCESS_TEST_RESOURCES;

/**
 * A factory which creates the test data.
 *
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
@SuppressWarnings("UtilityClass")
class Given {

    static final String SPINE_PROTOBUF_PLUGIN_ID = "org.spine3.tools.protobuf-plugin";

    // prevent instantiation of this utility class
    private Given() {
    }

    /** Creates a project with all required tasks. */
    static Project newProject() {
        final Project project = ProjectBuilder.builder()
                                              .build();
        project.task(CLEAN.getValue());
        project.task(GENERATE_PROTO.getValue());
        project.task(GENERATE_TEST_PROTO.getValue());
        project.task(COMPILE_JAVA.getValue());
        project.task(COMPILE_TEST_JAVA.getValue());
        project.task(PROCESS_RESOURCES.getValue());
        project.task(PROCESS_TEST_RESOURCES.getValue());
        return project;
    }

    static String newUuid() {
        final String result = UUID.randomUUID()
                                  .toString();
        return result;
    }
}
