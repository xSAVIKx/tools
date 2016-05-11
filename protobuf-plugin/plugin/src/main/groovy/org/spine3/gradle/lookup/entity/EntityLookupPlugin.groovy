/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.gradle.lookup.entity;

import groovy.util.logging.Slf4j;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import static ProtoParser.ProtoFileMetadata;

/**
 * Finds packages with Protobuf files belonging to an entity (containing entity state, commands, etc)
 * and writes each package and the type of the entity to which it belongs to the {@code entities.properties} file.
 *
 * @author Alexander Litus
 */
@Slf4j
public class EntityLookupPlugin implements Plugin<Project> {

    private static final String PROPS_FILE_NAME = "entities.properties";

    private Project project;

    @Override
    void apply(Project project) {
        this.project = project;
        final Task findEntitiesTask = project.task("findEntities") << {
            findEntityFilesAndWriteProps("main");
        }
        final Task findTestEntitiesTask = project.task("findTestEntities") << {
            findEntityFilesAndWriteProps("test");
        }
        findEntitiesTask.dependsOn("compileJava");
        findTestEntitiesTask.dependsOn("compileTestJava");
        final Task processResources = project.getTasks().getByPath("processResources");
        final Task processTestResources = project.getTasks().getByPath("processTestResources");
        processResources.dependsOn(findEntitiesTask);
        processTestResources.dependsOn(findTestEntitiesTask);
    }

    private void findEntityFilesAndWriteProps(String mainOrTest) {
        final String projectPath = project.getProjectDir().getAbsolutePath();
        final EntityFilesFinder finder = new EntityFilesFinder(project, "${projectPath}/src/${mainOrTest}/proto");
        final List<ProtoFileMetadata> metadataList = finder.findFiles();
        if (metadataList.isEmpty()) {
            return;
        }
        final Map<String, String> propsMap = toPackagesMap(metadataList)
        final PropertiesWriter writer = new PropertiesWriter("${projectPath}/generated/${mainOrTest}/resources", PROPS_FILE_NAME);
        writer.write(propsMap);
    }

    private static Map<String, String> toPackagesMap(List<ProtoFileMetadata> metadataList) {
        final Map<String, String> result = new HashMap<>();
        for (ProtoFileMetadata metadata : metadataList) {
            final String pack = metadata.getProtoPackage();
            final String type = metadata.getEntityType();
            result.put(pack, type);
        }
        return result;
    }
}
