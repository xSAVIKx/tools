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

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue
import static org.spine3.gradle.Given.*

@SuppressWarnings("GroovyInstanceMethodNamingConvention")
class ProtobufPluginShould {

    private Project project
    private TaskContainer tasks

    static {
        /** Returns `true` if a task depends on a passed one, `false` otherwise. */
        Task.metaClass.dependsOnTask = { def task ->
            final Set<Task> dependsOn = getDependsOn()
            final boolean contains = dependsOn.contains(task)
            return contains
        }
    }

    @Before
    void setUp() {
        project = newProject()
        project.pluginManager.apply(SPINE_PLUGIN_ID)
        tasks = project.tasks
    }

    @Test
    void apply_to_project() {
        final Project project = newProject()
        project.pluginManager.apply(SPINE_PLUGIN_ID)
    }

    @Test
    void add_task_preClean() {
        assertNotNull(tasks.preClean)
        assertTrue(tasks.clean.dependsOnTask(tasks.preClean))
    }

    @Test
    void add_task_generateFailures() {
        final Task genFailures = tasks.generateFailures
        assertNotNull(genFailures)
        assertTrue(genFailures.dependsOnTask(GENERATE_PROTO))
        assertTrue(tasks.compileJava.dependsOnTask(genFailures))
    }

    @Test
    void add_task_generateTestFailures() {
        final Task genTestFailures = tasks.generateTestFailures
        assertNotNull(genTestFailures)
        assertTrue(genTestFailures.dependsOnTask(GENERATE_TEST_PROTO))
        assertTrue(tasks.compileTestJava.dependsOnTask(genTestFailures))
    }

    @Test
    void add_task_findEnrichments() {
        final Task find = tasks.findEnrichments
        assertNotNull(find)
        assertTrue(find.dependsOnTask(COMPILE_JAVA))
        assertTrue(tasks.processResources.dependsOnTask(find))
    }

    @Test
    void add_task_findTestEnrichments() {
        final Task find = tasks.findTestEnrichments
        assertNotNull(find)
        assertTrue(find.dependsOnTask("compileTestJava"))
        assertTrue(tasks.processTestResources.dependsOnTask(find))
    }

    @Test
    void add_task_mapProtoToJava() {
        final Task mapProto = tasks.mapProtoToJava
        assertNotNull(mapProto)
        assertTrue(mapProto.dependsOnTask(GENERATE_PROTO))
        assertTrue(tasks.processResources.dependsOnTask(mapProto))
    }

    @Test
    void add_task_mapTestProtoToJava() {
        final Task mapProto = tasks.mapTestProtoToJava
        assertNotNull(mapProto)
        assertTrue(mapProto.dependsOnTask(GENERATE_TEST_PROTO))
        assertTrue(tasks.processTestResources.dependsOnTask(mapProto))
    }
}
