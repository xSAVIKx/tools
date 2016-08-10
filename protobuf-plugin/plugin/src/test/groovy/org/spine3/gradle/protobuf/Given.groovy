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

package org.spine3.gradle.protobuf

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * A factory which creates the test data.
 */
class Given {

    static final String SPINE_PROTOBUF_PLUGIN_ID = 'org.spine3.tools.protobuf-plugin'

    /** The name of the `clean` task.*/
    static final String CLEAN = "clean"

    /** The name of the `processResources` task.*/
    static final String PROCESS_RESOURCES = "processResources"

    /** The name of the `processTestResources` task.*/
    static final String PROCESS_TEST_RESOURCES = "processTestResources"

    /** The name of the `generateProto` task.*/
    static final String GENERATE_PROTO = "generateProto"

    /** The name of the `generateTestProto` task. */
    static final String GENERATE_TEST_PROTO = "generateTestProto"

    /** The name of the `compileJava` task.*/
    static final String COMPILE_JAVA = "compileJava"

    /** The name of the `compileTestJava` task.*/
    static final String COMPILE_TEST_JAVA = "compileTestJava"

    /** Creates a project with all required tasks. */
    static Project newProject() {
        final Project project = ProjectBuilder.builder()
                .build()
        project.task(CLEAN)
        project.task(PROCESS_RESOURCES)
        project.task(PROCESS_TEST_RESOURCES)
        project.task(COMPILE_JAVA)
        project.task(COMPILE_TEST_JAVA)
        return project
    }

    static GString newUuid() {
        final String id = UUID.randomUUID().toString()
        return "${id}"
    }
}
