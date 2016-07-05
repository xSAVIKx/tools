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
import org.spine3.gradle.lookup.entity.PropertiesWriter

import static com.google.protobuf.DescriptorProtos.FileDescriptorProto
import static com.google.protobuf.DescriptorProtos.FileDescriptorSet

/**
 * Finds event enrichment Protobuf definitions; creates a {@code .properties} file, which contains entries like:
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

    private static final String PROPS_FILE_NAME = "enrichments.properties";

    public static final String MSG_ENABLE_DESCRIPTOR_SET_GENERATION =
            "Please enable descriptor set generation. See an appropriate section at " +
            "https://github.com/google/protobuf-gradle-plugin/blob/master/README.md#customize-code-generation-tasks"

    private Project project;
    private String projectPath;

    @Override
    void apply(Project project) {
        this.project = project;
        this.projectPath = project.getProjectDir().getAbsolutePath();
        final Task findEnrichmentsTask = project.task("findEnrichments") << {
            findEnrichmentsAndWriteProps("main", true);
        }
        findEnrichmentsTask.dependsOn("compileJava");
        final Task processResources = project.getTasks().getByPath("processResources");
        processResources.dependsOn(findEnrichmentsTask);

        final Task findTestEnrichmentsTask = project.task("findTestEnrichments") << {
            findEnrichmentsAndWriteProps("test", false);
        }
        findTestEnrichmentsTask.dependsOn("compileTestJava");
        final Task processTestResources = project.getTasks().getByPath("processTestResources");
        processTestResources.dependsOn(findTestEnrichmentsTask);
    }

    private void findEnrichmentsAndWriteProps(String mainOrTest, boolean enableWarn) {
        final Map<String, String> propsMap = new HashMap<>();
        final List<FileDescriptorProto> files = getFileDescriptors(mainOrTest, enableWarn);
        for (FileDescriptorProto file : files) {
            final Map<String, String> enrichments = new EnrichmentsFinder(file).findEnrichments();
            propsMap.putAll(enrichments);
        }
        if (propsMap.isEmpty()) {
            return;
        }
        final PropertiesWriter writer = new PropertiesWriter(
                "${projectPath}/generated/${mainOrTest}/resources",
                PROPS_FILE_NAME);
        writer.write(propsMap);
    }

    private List<FileDescriptorProto> getFileDescriptors(String mainOrTest, boolean enableWarn) {
        final String descFilePath = "${projectPath}/build/descriptors/${mainOrTest}.desc";
        if (!new File(descFilePath).exists()) {
            if (enableWarn) {
                log.warn(MSG_ENABLE_DESCRIPTOR_SET_GENERATION);
            }
            return Collections.emptyList();
        }
        final List<FileDescriptorProto> fileDescriptors = new LinkedList<>();
        new FileInputStream(descFilePath).withStream {
            final FileDescriptorSet descriptorSet = FileDescriptorSet.parseFrom(it);
            descriptorSet.fileList.each { FileDescriptorProto descriptor ->
                fileDescriptors.add(descriptor);
            }
        }
        return fileDescriptors;
    }
}
