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
package org.spine3.gradle.protobuf;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskContainer;
import org.junit.Before;
import org.junit.Test;
import org.spine3.gradle.TaskName;

import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.gradle.TaskName.CLEAN;
import static org.spine3.gradle.TaskName.COMPILE_JAVA;
import static org.spine3.gradle.TaskName.COMPILE_TEST_JAVA;
import static org.spine3.gradle.TaskName.FIND_ENRICHMENTS;
import static org.spine3.gradle.TaskName.FIND_TEST_ENRICHMENTS;
import static org.spine3.gradle.TaskName.GENERATE_FAILURES;
import static org.spine3.gradle.TaskName.GENERATE_PROTO;
import static org.spine3.gradle.TaskName.GENERATE_TEST_FAILURES;
import static org.spine3.gradle.TaskName.GENERATE_TEST_PROTO;
import static org.spine3.gradle.TaskName.MAP_PROTO_TO_JAVA;
import static org.spine3.gradle.TaskName.MAP_TEST_PROTO_TO_JAVA;
import static org.spine3.gradle.TaskName.PRE_CLEAN;
import static org.spine3.gradle.TaskName.PROCESS_RESOURCES;
import static org.spine3.gradle.TaskName.PROCESS_TEST_RESOURCES;
import static org.spine3.gradle.protobuf.Given.SPINE_PROTOBUF_PLUGIN_ID;
import static org.spine3.gradle.protobuf.Given.newProject;

/**
 * @author Alex Tymchenko
 */
public class ProtobufPluginShould {

    private Project project;
    private TaskContainer tasks;

    @Before
    public void setUp() {
        project = newProject();
        project.getPluginManager()
               .apply(SPINE_PROTOBUF_PLUGIN_ID);
        tasks = project.getTasks();
    }

    @Test
    public void apply_to_project() {
        final Project project = newProject();
        project.getPluginManager()
               .apply(SPINE_PROTOBUF_PLUGIN_ID);
    }

    @Test
    public void add_task_preClean() {
        assertNotNull(task(PRE_CLEAN));
        assertTrue(dependsOn(task(CLEAN), task(PRE_CLEAN)));
    }

    @Test
    public void add_task_generateFailures() {

        final Task genFailures = task(GENERATE_FAILURES);
        assertNotNull(genFailures);
        assertTrue(dependsOn(genFailures, GENERATE_PROTO));
        assertTrue(dependsOn(task(COMPILE_JAVA), genFailures));
    }

    @Test
    public void add_task_generateTestFailures() {
        final Task genTestFailures = task(GENERATE_TEST_FAILURES);
        assertNotNull(genTestFailures);
        assertTrue(dependsOn(genTestFailures, GENERATE_TEST_PROTO));
        assertTrue(dependsOn(task(COMPILE_TEST_JAVA), genTestFailures));
    }

    @Test
    public void add_task_findEnrichments() {
        final Task find = task(FIND_ENRICHMENTS);
        assertNotNull(find);
        assertTrue(dependsOn(find, COMPILE_JAVA));
        assertTrue(dependsOn(task(PROCESS_RESOURCES), find));
    }

    @Test
    public void add_task_findTestEnrichments() {
        final Task find = task(FIND_TEST_ENRICHMENTS);
        assertNotNull(find);
        assertTrue(dependsOn(find, COMPILE_TEST_JAVA));
        assertTrue(dependsOn(task(PROCESS_TEST_RESOURCES), find));
    }

    @Test
    public void add_task_mapProtoToJava() {
        final Task mapProto = task(MAP_PROTO_TO_JAVA);
        assertNotNull(mapProto);
        assertTrue(dependsOn(mapProto, GENERATE_PROTO));
        assertTrue(dependsOn(task(PROCESS_RESOURCES), mapProto));
    }

    @Test
    public void add_task_mapTestProtoToJava() {
        final Task mapProto = task(MAP_TEST_PROTO_TO_JAVA);
        assertNotNull(mapProto);
        assertTrue(dependsOn(mapProto, GENERATE_TEST_PROTO));
        assertTrue(dependsOn(task(PROCESS_TEST_RESOURCES), mapProto));
    }

    private Task task(TaskName taskName) {
        return tasks.getByName(taskName.getValue());
    }

    private static boolean dependsOn(Task task, Task ontoTask) {
        return dependsOn(task, ontoTask.getName());
    }

    private static boolean dependsOn(Task task, TaskName ontoTaskWithName) {
        final String taskName = ontoTaskWithName.getValue();
        return dependsOn(task, taskName);
    }

    /**
     * As long as we are dealing with Gradle Groovy-based API, we have to use `instanceof` to analyze `Object[]`
     * values returned.
     **/
    @SuppressWarnings("ChainOfInstanceofChecks")
    private static boolean dependsOn(Task task, String ontoTaskWithName) {
        final Set<Object> dependsOn = task.getDependsOn();

        boolean contains = false;
        for (Object anObject : dependsOn) {
            if (anObject instanceof String) {
                contains = contains || ontoTaskWithName.equals(anObject);
            }
            if (anObject instanceof Task) {
                final Task objectAsTask = (Task) anObject;
                contains = contains || ontoTaskWithName.equals(objectAsTask.getName());
            }
        }
        return contains;
    }
}
