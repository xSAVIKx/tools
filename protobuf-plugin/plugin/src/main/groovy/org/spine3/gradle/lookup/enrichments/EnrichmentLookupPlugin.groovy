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

package org.spine3.gradle.lookup.enrichments

import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.spine3.gradle.util.PropertiesWriter

import static com.google.protobuf.DescriptorProtos.FileDescriptorProto
import static org.spine3.gradle.Extension.*
import static org.spine3.gradle.util.DescriptorSetUtil.getProtoFileDescriptors

/**
 * Finds event enrichment Protobuf definitions and creates a {@code .properties} file, which contains entries like:
 *
 * <p>{@code ENRICHMENT_TYPE_NAME=EVENT_TO_ENRICH_TYPE_NAME}
 *
 * There can be several event types:
 *
 * <p>{@code ENRICHMENT_TYPE_NAME=FIRST_EVENT_TYPE_NAME,SECOND_EVENT_TYPE_NAME}
 *
 * @author Alexander Litus
 */
@Slf4j
public class EnrichmentLookupPlugin implements Plugin<Project> {

    /**
     * The name of the file to populate. NOTE: also change its name used in the `core-java` project on changing.
     */
    private static final String PROPS_FILE_NAME = "enrichments.properties"

    @Override
    void apply(Project project) {
        final Task findEnrichmentsTask = project.task("findEnrichments") << {
            findEnrichmentsAndWriteProps(getMainTargetGenResourcesDir(project), getMainDescriptorSetPath(project))
        }
        findEnrichmentsTask.dependsOn("compileJava")
        final Task processResources = project.getTasks().getByPath("processResources")
        processResources.dependsOn(findEnrichmentsTask)

        final Task findTestEnrichmentsTask = project.task("findTestEnrichments") << {
            findEnrichmentsAndWriteProps(getTestTargetGenResourcesDir(project), getTestDescriptorSetPath(project))
        }
        findTestEnrichmentsTask.dependsOn("compileTestJava")
        final Task processTestResources = project.getTasks().getByPath("processTestResources")
        processTestResources.dependsOn(findTestEnrichmentsTask)
    }

    private static void findEnrichmentsAndWriteProps(GString targetGeneratedResourcesDir, GString descriptorSetPath) {
        final Map<GString, GString> propsMap = new HashMap<>()
        final List<FileDescriptorProto> files = getProtoFileDescriptors(descriptorSetPath)
        for (FileDescriptorProto file : files) {
            final Map<GString, GString> enrichments = new EnrichmentsFinder(file).findEnrichments()
            propsMap.putAll(enrichments)
        }
        if (propsMap.isEmpty()) {
            return
        }
        final PropertiesWriter writer = new PropertiesWriter(targetGeneratedResourcesDir, PROPS_FILE_NAME)
        writer.write(propsMap)
    }
}
