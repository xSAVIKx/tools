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
package org.spine3.gradle.protobuf.lookup.proto;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import groovy.lang.GString;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.spine3.gradle.protobuf.util.DescriptorSetUtil;
import org.spine3.gradle.protobuf.util.PropertiesWriter;

import java.util.Collection;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.spine3.gradle.GradleTasks.GENERATE_PROTO;
import static org.spine3.gradle.GradleTasks.GENERATE_TEST_PROTO;
import static org.spine3.gradle.GradleTasks.MAP_PROTO_TO_JAVA;
import static org.spine3.gradle.GradleTasks.MAP_TEST_PROTO_TO_JAVA;
import static org.spine3.gradle.GradleTasks.PROCESS_RESOURCES;
import static org.spine3.gradle.GradleTasks.PROCESS_TEST_RESOURCES;
import static org.spine3.gradle.protobuf.Extension.getMainDescriptorSetPath;
import static org.spine3.gradle.protobuf.Extension.getMainTargetGenResourcesDir;
import static org.spine3.gradle.protobuf.Extension.getTestDescriptorSetPath;
import static org.spine3.gradle.protobuf.Extension.getTestTargetGenResourcesDir;
import static org.spine3.gradle.protobuf.util.DescriptorSetUtil.getProtoFileDescriptors;

/**
 * Plugin which maps all Protobuf types to the corresponding Java classes.
 *
 * <p>Generates a {@code .properties} file, which contains entries like:
 *
 * <p>{@code PROTO_TYPE_URL=JAVA_FULL_CLASS_NAME}
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
public class ProtoToJavaMapperPlugin implements Plugin<Project> {

    /**
     * The name of the file to populate. NOTE: also change its name used in the `core-java` project on changing.
     */
    private static final String PROPERTIES_FILE_NAME = "known_types.properties";

    /**
     * Adds tasks to map Protobuf types to Java classes in the project.
     */
    @Override
    public void apply(final Project project) {
        final Action<Task> mainSrcAction = new Action<Task>() {
            @Override
            public void execute(Task task) {
                mapProtoToJavaAndWriteProps(getMainTargetGenResourcesDir(project), getMainDescriptorSetPath(project));
            }
        };
        final Task mapProtoToJava = project.task(MAP_PROTO_TO_JAVA.getName())
                                           .doLast(mainSrcAction);
        mapProtoToJava.dependsOn(GENERATE_PROTO.getName());
        final Task processResources = project.getTasks()
                                             .getByPath(PROCESS_RESOURCES.getName());
        processResources.dependsOn(mapProtoToJava);

        final Action<Task> testSrcAction = new Action<Task>() {
            @Override
            public void execute(Task task) {
                mapProtoToJavaAndWriteProps(getTestTargetGenResourcesDir(project), getTestDescriptorSetPath(project));
            }
        };
        final Task mapTestProtoToJava = project.task(MAP_TEST_PROTO_TO_JAVA.getName())
                                               .doLast(testSrcAction);
        mapTestProtoToJava.dependsOn(GENERATE_TEST_PROTO.getName());
        final Task processTestResources = project.getTasks()
                                                 .getByPath(PROCESS_TEST_RESOURCES.getName());
        processTestResources.dependsOn(mapTestProtoToJava);
    }

    @SuppressWarnings("MethodParameterNamingConvention")
    private static void mapProtoToJavaAndWriteProps(GString targetGeneratedResourcesDir, GString descriptorSetPath) {
        final Map<String, String> propsMap = newHashMap();
        final Collection<FileDescriptorProto> files = getProtoFileDescriptors(descriptorSetPath, new DescriptorSetUtil.IsNotGoogleProto());
        for (FileDescriptorProto file : files) {
            final Map<String, String> types = new ProtoToJavaTypeMapper(file).mapTypes();
            propsMap.putAll(types);
        }
        if (propsMap.isEmpty()) {
            return;
        }
        final PropertiesWriter writer = new PropertiesWriter(targetGeneratedResourcesDir, PROPERTIES_FILE_NAME);
        writer.write(propsMap);
    }

}
