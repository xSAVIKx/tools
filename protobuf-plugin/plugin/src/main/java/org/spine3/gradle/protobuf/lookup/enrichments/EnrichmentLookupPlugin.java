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
package org.spine3.gradle.protobuf.lookup.enrichments;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.gradle.SpinePlugin;
import org.spine3.gradle.protobuf.util.DescriptorSetUtil;
import org.spine3.gradle.protobuf.util.PropertiesWriter;

import java.util.Collection;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.spine3.gradle.TaskName.COMPILE_JAVA;
import static org.spine3.gradle.TaskName.COMPILE_TEST_JAVA;
import static org.spine3.gradle.TaskName.FIND_ENRICHMENTS;
import static org.spine3.gradle.TaskName.FIND_TEST_ENRICHMENTS;
import static org.spine3.gradle.TaskName.PROCESS_RESOURCES;
import static org.spine3.gradle.TaskName.PROCESS_TEST_RESOURCES;
import static org.spine3.gradle.protobuf.Extension.getMainDescriptorSetPath;
import static org.spine3.gradle.protobuf.Extension.getMainTargetGenResourcesDir;
import static org.spine3.gradle.protobuf.Extension.getTestDescriptorSetPath;
import static org.spine3.gradle.protobuf.Extension.getTestTargetGenResourcesDir;
import static org.spine3.gradle.protobuf.util.DescriptorSetUtil.getProtoFileDescriptors;

/**
 * Finds event enrichment Protobuf definitions and creates a {@code .properties} file,
 * which contains entries like:
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
public class EnrichmentLookupPlugin extends SpinePlugin {
    /**
     * The name of the file to populate.
     *
     * <p>NOTE: the filename is referenced by `core-java` as well,
     * make sure to update `core-java` project upon changing this value.
     */
    private static final String PROPS_FILE_NAME = "enrichments.properties";

    @Override
    public void apply(final Project project) {
        final Action<Task> mainScopeAction = mainScopeActionFor(project);
        logDependingTask(log(), FIND_ENRICHMENTS, PROCESS_RESOURCES, COMPILE_JAVA);
        final GradleTask findEnrichments =
                newTask(FIND_ENRICHMENTS,
                        mainScopeAction).insertAfterTask(COMPILE_JAVA)
                                        .insertBeforeTask(PROCESS_RESOURCES)
                                        .applyNowTo(project);
        final Action<Task> testScopeAction = testScopeActionFor(project);
        logDependingTask(log(), FIND_TEST_ENRICHMENTS, PROCESS_TEST_RESOURCES, COMPILE_TEST_JAVA);
        final GradleTask findTestEnrichments =
                newTask(FIND_TEST_ENRICHMENTS,
                        testScopeAction).insertAfterTask(COMPILE_TEST_JAVA)
                                        .insertBeforeTask(PROCESS_TEST_RESOURCES)
                                        .applyNowTo(project);

        final String msg = "Enrichment lookup phase initialized with tasks: {}, {}";
        log().debug(msg, findEnrichments, findTestEnrichments);
    }

    private static Action<Task> testScopeActionFor(final Project project) {
        log().debug("Initializing the enrichment lookup for the \"test\" source code");
        return new Action<Task>() {
            @Override
            public void execute(Task task) {
                findEnrichmentsAndWriteProps(getTestTargetGenResourcesDir(project),
                                             getTestDescriptorSetPath(project));
            }
        };
    }

    private static Action<Task> mainScopeActionFor(final Project project) {
        log().debug("Initializing the enrichment lookup for the \"main\" source code");
        return new Action<Task>() {
            @Override
            public void execute(Task task) {
                findEnrichmentsAndWriteProps(getMainTargetGenResourcesDir(project),
                                             getMainDescriptorSetPath(project));
            }
        };
    }

    private static void findEnrichmentsAndWriteProps(
            // It's important to have a self-explanatory name for this variable.
            @SuppressWarnings("MethodParameterNamingConvention") String targetGeneratedResourcesDir,
            String descriptorSetPath) {
        log().debug("Enrichment lookup started");

        final Map<String, String> propsMap = newHashMap();
        final DescriptorSetUtil.IsNotGoogleProto protoFilter =
                new DescriptorSetUtil.IsNotGoogleProto();
        final Collection<FileDescriptorProto> files = getProtoFileDescriptors(descriptorSetPath,
                                                                              protoFilter);
        for (FileDescriptorProto file : files) {
            final Map<String, String> enrichments = new EnrichmentsFinder(file).findEnrichments();
            propsMap.putAll(enrichments);
        }
        if (propsMap.isEmpty()) {
            log().debug("Enrichment lookup complete. No enrichments found.");
            return;
        }

        log().debug("Writing the enrichment description to {}/{}",
                    targetGeneratedResourcesDir, PROPS_FILE_NAME);
        final PropertiesWriter writer =
                new PropertiesWriter(targetGeneratedResourcesDir, PROPS_FILE_NAME);
        writer.write(propsMap);

        log().debug("Enrichment lookup complete");
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EnrichmentLookupPlugin.class);
    }
}
