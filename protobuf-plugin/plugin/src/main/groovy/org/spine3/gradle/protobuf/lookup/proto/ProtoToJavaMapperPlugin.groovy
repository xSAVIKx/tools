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

package org.spine3.gradle.protobuf.lookup.proto

import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.spine3.gradle.protobuf.util.PropertiesWriter

import static com.google.protobuf.DescriptorProtos.FileDescriptorProto
import static org.spine3.gradle.protobuf.Extension.*
import static org.spine3.gradle.protobuf.util.DescriptorSetUtil.getProtoFileDescriptors

/**
 * Plugin which maps all Protobuf types to the corresponding Java classes.
 *
 * <p>Generates a {@code .properties} file, which contains entries like:
 *
 * <p>{@code PROTO_TYPE_URL=JAVA_FULL_CLASS_NAME}
 */
@Slf4j
class ProtoToJavaMapperPlugin implements Plugin<Project> {

    /**
     * The name of the file to populate. NOTE: also change its name used in the `core-java` project on changing.
     */
    private static final String PROPERTIES_FILE_NAME = "known_types.properties"

    /**
     * Adds tasks to map Protobuf types to Java classes in the project.
     */
    @Override
    void apply(Project project) {
        final Task mapProtoToJava = project.task("mapProtoToJava") << {
            mapProtoToJavaAndWriteProps(getMainTargetGenResourcesDir(project), getMainDescriptorSetPath(project))
        }
        mapProtoToJava.dependsOn("generateProto")
        final Task processResources = project.tasks.getByPath("processResources")
        processResources.dependsOn(mapProtoToJava)

        final Task mapTestProtoToJava = project.task("mapTestProtoToJava") << {
            mapProtoToJavaAndWriteProps(getTestTargetGenResourcesDir(project), getTestDescriptorSetPath(project))
        }
        mapTestProtoToJava.dependsOn("generateTestProto")
        final Task processTestResources = project.tasks.getByPath("processTestResources")
        processTestResources.dependsOn(mapTestProtoToJava)
    }

    private static void mapProtoToJavaAndWriteProps(GString targetGeneratedResourcesDir, GString descriptorSetPath) {
        final Map<GString, GString> propsMap = new HashMap<>()
        final Collection<FileDescriptorProto> files = getProtoFileDescriptors(descriptorSetPath)
        for (FileDescriptorProto file : files) {
            if (!file.package.contains("google")) { // TODO:2016-08-04:alexander.litus: improve
                final Map<GString, GString> enrichments = new ProtoToJavaTypeMapper(file).mapTypes()
                propsMap.putAll(enrichments)
            }
        }
        if (propsMap.isEmpty()) {
            return
        }
        final PropertiesWriter writer = new PropertiesWriter(targetGeneratedResourcesDir, PROPERTIES_FILE_NAME)
        writer.write(propsMap)
    }
}
