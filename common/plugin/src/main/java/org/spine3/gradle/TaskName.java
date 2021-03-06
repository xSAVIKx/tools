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
package org.spine3.gradle;

import com.google.common.base.MoreObjects;

/**
 * Task names in Gradle build lifecycle.
 *
 * <p>Spine `tools` library uses some of 3rd-party Gradle tasks as anchors for own execution.
 *
 * @author Alex Tymchenko
 */
public enum TaskName {

    /**
     * Gradle-own and 3rd-party task names.
     **********************/

    CLEAN("clean"),

    BUILD("build"),

    COMPILE_JAVA("compileJava"),
    COMPILE_TEST_JAVA("compileTestJava"),
    CLASSES("classes"),

    GENERATE_PROTO("generateProto"),
    GENERATE_TEST_PROTO("generateTestProto"),

    PROCESS_RESOURCES("processResources"),
    PROCESS_TEST_RESOURCES("processTestResources"),

    /**
     * Spine custom task names.
     **********************/

    /**
     * The name of the additional cleanup task added to the Gradle lifecycle.
     */
    PRE_CLEAN("preClean"),

    /**
     * The name of the task that checks for wrong FQN naming in javadocs.
     *
     * <p>Relates only to `main` classes.
     */
    CHECK_FQN("checkJavadocLink"),

    /**
     * The name of the failure generation task added to the Gradle lifecycle.
     *
     * <p>Relates to `main` classes and resources scope.
     */
    GENERATE_FAILURES("generateFailures"),

    /**
     * The name of the failure generation task added to the Gradle lifecycle.
     *
     * <p>Relates to `test` classes and resources scope.
     */
    GENERATE_TEST_FAILURES("generateTestFailures"),

    /**
     * The name of the enrichment lookup task added to the Gradle lifecycle.
     *
     * <p>Relates to `main` classes and resources scope.
     */
    FIND_ENRICHMENTS("findEnrichments"),

    /**
     * The name of the enrichment lookup task added to the Gradle lifecycle.
     *
     * <p>Relates to `test` classes and resources scope.
     */
    FIND_TEST_ENRICHMENTS("findTestEnrichments"),

    /**
     * The name of the {@code .proto}-to-Java mapping task added to the Gradle lifecycle.
     *
     * <p>Relates to `main` classes and resources scope.
     */
    MAP_PROTO_TO_JAVA("mapProtoToJava"),

    /**
     * The name of the {@code .proto}-to-Java mapping task added to the Gradle lifecycle.
     *
     * <p>Relates to `test` classes and resources scope.
     */
    MAP_TEST_PROTO_TO_JAVA("mapTestProtoToJava"),

    /**
     * The name of the class path scan task added to the Gradle lifecycle.
     */
    SCAN_CLASS_PATH("scanClassPath");

    TaskName(String value) {
        this.value = value;
    }

    private final String value;

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("val", value)
                          .toString();
    }
}
