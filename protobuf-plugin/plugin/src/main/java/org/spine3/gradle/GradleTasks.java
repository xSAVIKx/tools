/*
 *
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
 *
 */
package org.spine3.gradle;

import com.google.common.base.MoreObjects;

/**
 * Task names in Gradle build lifecycle.
 *
 * <p>Spine `tools` library uses some of 3rd-party Gradle tasks as anchors for own execution.
 *
 * @author Alex Tymchenko
 */
public enum GradleTasks {

    CLEAN("clean"),

    COMPILE_JAVA("compileJava"),
    COMPILE_TEST_JAVA("compileTestJava"),

    GENERATE_PROTO("generateProto"),
    GENERATE_TEST_PROTO("generateTestProto"),

    PROCESS_RESOURCES("processResources"),
    PROCESS_TEST_RESOURCES("processTestResources"),

    /**
     * Spine tasks
     **********************/

    PRE_CLEAN("preClean"),

    GENERATE_FAILURES("generateFailures"),
    GENERATE_TEST_FAILURES("generateTestFailures"),

    /**
     * The name of the enrichment lookup task to be added to the Gradle lifecycle.
     *
     * <p>Relates to `main` classes and resources scope.
     */
    FIND_ENRICHMENTS("findEnrichments"),

    /**
     * The name of the enrichment lookup task to be added to the Gradle lifecycle.
     *
     * <p>Relates to `test` classes and resources scope.
     */
    FIND_TEST_ENRICHMENTS("findTestEnrichments"),

    MAP_PROTO_TO_JAVA("mapProtoToJava"),
    MAP_TEST_PROTO_TO_JAVA("mapTestProtoToJava");

    GradleTasks(String name) {
        this.name = name;
    }

    private final String name;

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("name", name)
                          .toString();
    }
}
