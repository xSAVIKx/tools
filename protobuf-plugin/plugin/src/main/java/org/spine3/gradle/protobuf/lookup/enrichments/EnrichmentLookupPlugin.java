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
package org.spine3.gradle.protobuf.lookup.enrichments;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import groovy.lang.GString;
import groovy.util.logging.Slf4j;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.spine3.gradle.protobuf.util.DescriptorSetUtil;
import org.spine3.gradle.protobuf.util.PropertiesWriter;

import java.util.Collection;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.spine3.gradle.GradleTasks.COMPILE_JAVA;
import static org.spine3.gradle.GradleTasks.COMPILE_TEST_JAVA;
import static org.spine3.gradle.GradleTasks.FIND_ENRICHMENTS;
import static org.spine3.gradle.GradleTasks.FIND_TEST_ENRICHMENTS;
import static org.spine3.gradle.GradleTasks.PROCESS_RESOURCES;
import static org.spine3.gradle.GradleTasks.PROCESS_TEST_RESOURCES;
import static org.spine3.gradle.protobuf.Extension.getMainDescriptorSetPath;
import static org.spine3.gradle.protobuf.Extension.getMainTargetGenResourcesDir;
import static org.spine3.gradle.protobuf.Extension.getTestDescriptorSetPath;
import static org.spine3.gradle.protobuf.Extension.getTestTargetGenResourcesDir;
import static org.spine3.gradle.protobuf.util.DescriptorSetUtil.getProtoFileDescriptors;

/**
 * Finds event enrichment Protobuf definitions and creates a {@code .properties} file, which contains entries like:
 *
 * <p>{@code ENRICHMENT_TYPE_NAME=EVENT_TO_ENRICH_TYPE_NAME}
 *
 * <p>There can be several event types:
 *
 * <p>{@code ENRICHMENT_TYPE_NAME=FIRST_EVENT_TYPE_NAME,SECOND_EVENT_TYPE_NAME}
 *
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
@Slf4j
public class EnrichmentLookupPlugin implements Plugin<Project> {
    /**
     * The name of the file to populate.
     *
     * <p>NOTE: the filename is referenced by `core-java` as well,
     * make sure to update `core-java` project upon changing this value.
     */
    private static final String PROPS_FILE_NAME = "enrichments.properties";

    @Override
    public void apply(final Project project) {
        final Action<Task> findEnrichmentsAction = mainScopeActionFor(project);
        addToProjectLifecycle(project, findEnrichmentsAction, FIND_ENRICHMENTS.getName(),
                              COMPILE_JAVA.getName(), PROCESS_RESOURCES.getName());

        final Action<Task> findTestEnrichmentsAction = testScopeActionFor(project);
        addToProjectLifecycle(project, findTestEnrichmentsAction, FIND_TEST_ENRICHMENTS.getName(),
                              COMPILE_TEST_JAVA.getName(), PROCESS_TEST_RESOURCES.getName());
    }

    private static Action<Task> testScopeActionFor(final Project project) {
        return new Action<Task>() {
            @Override
            public void execute(Task task) {
                findEnrichmentsAndWriteProps(getTestTargetGenResourcesDir(project),
                                             getTestDescriptorSetPath(project));
            }
        };
    }

    private static Action<Task> mainScopeActionFor(final Project project) {
        return new Action<Task>() {
            @Override
            public void execute(Task task) {
                findEnrichmentsAndWriteProps(getMainTargetGenResourcesDir(project),
                                             getMainDescriptorSetPath(project));
            }
        };
    }

    private static void addToProjectLifecycle(Project project, Action<Task> taskAction,
                                              String newTaskName, String taskBeforeNew, String taskAfterNew) {
        final Task findEnrichmentsTask = project.task(newTaskName)
                                                .doLast(taskAction);
        findEnrichmentsTask.dependsOn(taskBeforeNew);
        final Task processResources = project.getTasks()
                                             .getByPath(taskAfterNew);
        processResources.dependsOn(findEnrichmentsTask);
    }

    private static void findEnrichmentsAndWriteProps(
            // It's important to have a self-explanatory name for this variable.
            @SuppressWarnings("MethodParameterNamingConvention") GString targetGeneratedResourcesDir,
            GString descriptorSetPath) {
        final Map<String, String> propsMap = newHashMap();
        final DescriptorSetUtil.IsNotGoogleProto protoFilter = new DescriptorSetUtil.IsNotGoogleProto();
        final Collection<FileDescriptorProto> files = getProtoFileDescriptors(descriptorSetPath,
                                                                              protoFilter);
        for (FileDescriptorProto file : files) {
            final Map<String, String> enrichments = new EnrichmentsFinder(file).findEnrichments();
            propsMap.putAll(enrichments);
        }
        if (propsMap.isEmpty()) {
            return;
        }
        final PropertiesWriter writer = new PropertiesWriter(targetGeneratedResourcesDir, PROPS_FILE_NAME);
        writer.write(propsMap);
    }
}
