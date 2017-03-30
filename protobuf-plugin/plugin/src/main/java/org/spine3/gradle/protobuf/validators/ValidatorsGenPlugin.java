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

package org.spine3.gradle.protobuf.validators;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProtoOrBuilder;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.gradle.SpinePlugin;
import org.spine3.gradle.protobuf.failure.MessageTypeCache;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.spine3.gradle.TaskName.COMPILE_JAVA;
import static org.spine3.gradle.TaskName.COMPILE_TEST_JAVA;
import static org.spine3.gradle.TaskName.GENERATE_PROTO;
import static org.spine3.gradle.TaskName.GENERATE_TEST_PROTO;
import static org.spine3.gradle.TaskName.GENERATE_TEST_VALIDATING_BUILDERS;
import static org.spine3.gradle.TaskName.GENERATE_VALIDATING_BUILDERS;
import static org.spine3.gradle.protobuf.Extension.getMainDescriptorSetPath;
import static org.spine3.gradle.protobuf.Extension.getTargetGenValidatorsRootDir;
import static org.spine3.gradle.protobuf.Extension.getTargetTestGenValidatorsRootDir;
import static org.spine3.gradle.protobuf.Extension.getTestDescriptorSetPath;
import static org.spine3.gradle.protobuf.util.DescriptorSetUtil.getProtoFileDescriptors;
import static org.spine3.gradle.protobuf.util.GenerationUtils.getMessageName;
import static org.spine3.gradle.protobuf.util.GenerationUtils.isMap;
import static org.spine3.gradle.protobuf.util.GenerationUtils.isMessage;

/**
 * Plugin which generates validator builders, based on commands.proto files.
 *
 * @author Illia Shepilov
 */
public class ValidatorsGenPlugin extends SpinePlugin {

    private static final String JAVA_CLASS_NAME_SUFFIX = "Validator";

    /** A map from Protobuf type name to Protobuf DescriptorProto. */
    private final Map<String, DescriptorProto> allMessageDescriptors = newHashMap();

    /** A map from Protobuf type name to Protobuf FileDescriptorProto. */
    private final FileDescriptorCache descriptorCache = FileDescriptorCache.getInstance();

    /** A map from Protobuf type name to Java class FQN. */
    private final MessageTypeCache messageTypeCache = new MessageTypeCache();

    @Override
    public void apply(Project project) {
        log().debug("Preparing to generate validating builders");
        final Action<Task> mainScopeAction =
                createAction(getMainDescriptorSetPath(project),
                             getTargetGenValidatorsRootDir(project));

        logDependingTask(log(), GENERATE_VALIDATING_BUILDERS, COMPILE_JAVA, GENERATE_PROTO);
        final GradleTask generateValidator =
                newTask(GENERATE_VALIDATING_BUILDERS,
                        mainScopeAction).insertAfterTask(GENERATE_PROTO)
                                        .insertBeforeTask(COMPILE_JAVA)
                                        .applyNowTo(project);
        log().debug("Preparing to generate test validating builders");
        final Action<Task> testScopeAction =
                createAction(getTestDescriptorSetPath(project),
                             getTargetTestGenValidatorsRootDir(project));

        logDependingTask(log(), GENERATE_TEST_VALIDATING_BUILDERS,
                         COMPILE_TEST_JAVA, GENERATE_TEST_PROTO);
        final GradleTask generateTestValidator =
                newTask(GENERATE_TEST_VALIDATING_BUILDERS,
                        testScopeAction).insertAfterTask(GENERATE_TEST_PROTO)
                                        .insertBeforeTask(COMPILE_TEST_JAVA)
                                        .applyNowTo(project);
        log().debug("Validating builders generation phase initialized with tasks: {}, {}",
                    generateValidator,
                    generateTestValidator);
    }

    private Action<Task> createAction(final String path, final String targetDir) {
        return new Action<Task>() {
            @Override
            public void execute(Task task) {
                log().debug("Generating the validators from {}", path);
                final Set<ValidatorMetadata> metadatas = process(path);
                for (ValidatorMetadata metadata : metadatas) {
                    new ValidatorWriter(metadata, targetDir, messageTypeCache).write();
                }
            }
        };
    }

    private Set<ValidatorMetadata> process(String path) {
        log().debug("Obtaining the metadata for all validators");
        final List<FileDescriptorProto> fileDescriptors = getCommandProtoFileDescriptors(path);
        final Set<ValidatorMetadata> metadataSet = obtainFileMetadataValidators(fileDescriptors);
        final Set<ValidatorMetadata> result = obtainAllMetadataValidators(metadataSet);
        log().debug("The metadata is obtained, will be constructed {} validator(s)", result.size());
        return result;
    }

    private Set<ValidatorMetadata> obtainFileMetadataValidators(
            Iterable<FileDescriptorProto> fileDescriptors) {
        log().debug("Obtaining the metadata for the command validators");
        final Set<ValidatorMetadata> result = newHashSet();
        for (FileDescriptorProto file : fileDescriptors) {
            final List<DescriptorProto> fieldDescriptors = file.getMessageTypeList();
            final Set<ValidatorMetadata> metadataSet =
                    constructMessageFieldMetadata(fieldDescriptors);
            result.addAll(metadataSet);
        }
        log().debug("The metadata is obtained.");
        return result;
    }

    private Set<ValidatorMetadata> obtainAllMetadataValidators(
            Iterable<ValidatorMetadata> metadataSet) {
        log().debug("Obtaining the metadata for the validators, " +
                            "which will be constructed for the command fields");
        final Set<ValidatorMetadata> result = newHashSet();
        for (ValidatorMetadata metadata : metadataSet) {
            final DescriptorProto msgDescriptor = metadata.getMsgDescriptor();
            final List<DescriptorProto> descriptors = getFiledDescriptors(msgDescriptor);
            result.add(metadata);
            final Set<ValidatorMetadata> fieldMetadataSet =
                    constructMessageFieldMetadata(descriptors);
            result.addAll(fieldMetadataSet);
        }
        log().debug("The metadata for the field validators is obtained.");
        return result;
    }

    private Set<ValidatorMetadata> constructMessageFieldMetadata(
            Iterable<DescriptorProto> descriptors) {
        final Set<ValidatorMetadata> result = newHashSet();
        for (DescriptorProto descriptorMsg : descriptors) {
            final ValidatorMetadata metadata = createMetadata(descriptorMsg);
            result.add(metadata);
        }
        return result;
    }

    private ValidatorMetadata createMetadata(DescriptorProto msgDescriptor) {
        final String className = msgDescriptor.getName() + JAVA_CLASS_NAME_SUFFIX;
        final String javaPackage = descriptorCache.getJavaPackageFor(msgDescriptor.getName());
        final ValidatorMetadata result =
                new ValidatorMetadata(javaPackage, className, msgDescriptor);
        return result;
    }

    private List<DescriptorProto> getFiledDescriptors(DescriptorProto msgDescriptor) {
        final List<FieldDescriptorProto> fieldDescriptors = msgDescriptor.getFieldList();
        final List<DescriptorProto> descriptors = newArrayList();
        int index = 0;
        for (FieldDescriptorProto fieldDescriptor : fieldDescriptors) {
            if (!isMessage(fieldDescriptor)) {
                continue;
            }

            if (isMap(fieldDescriptor)) {
                final DescriptorProto descriptor = msgDescriptor.getNestedType(index);
                final FieldDescriptorProto keyDescriptor = descriptor.getField(0);
                final FieldDescriptorProto valueDescriptor = descriptor.getField(1);
                addDescriptor(descriptors, keyDescriptor);
                addDescriptor(descriptors, valueDescriptor);
                ++index;
                continue;
            }

            addDescriptor(descriptors, fieldDescriptor);
        }
        return descriptors;
    }

    private List<DescriptorProto> addDescriptor(List<DescriptorProto> descriptors,
                                                FieldDescriptorProto fileDescriptor) {
        final String msgName = getMessageName(fileDescriptor.getTypeName());
        final DescriptorProto fieldDescriptor = allMessageDescriptors.get(msgName);
        if (fieldDescriptor != null) {
            descriptors.add(fieldDescriptor);
        }
        return descriptors;
    }

    private List<FileDescriptorProto> getCommandProtoFileDescriptors(String descFilePath) {
        log().debug("Obtaining the file descriptors by {} path", descFilePath);
        final List<FileDescriptorProto> result = new LinkedList<>();
        final Collection<FileDescriptorProto> allDescriptors = getProtoFileDescriptors(descFilePath);
        for (FileDescriptorProto fileDescriptor : allDescriptors) {
            final boolean isCommandFile = fileDescriptor.getName()
                                              .endsWith("commands.proto");
            cacheAllMessageDescriptors(fileDescriptor);
            cacheFileDescriptors(fileDescriptor);
            messageTypeCache.cacheTypes(fileDescriptor);
            if (isCommandFile) {
                log().info("Found command file: {}", fileDescriptor.getName());
                result.add(fileDescriptor);
            }
        }
        log().debug("Found commands in files: {}", result);
        return result;
    }

    private void cacheAllMessageDescriptors(FileDescriptorProtoOrBuilder file) {
        List<DescriptorProto> descriptors = file.getMessageTypeList();
        for (DescriptorProto msgDescriptor : descriptors) {
            allMessageDescriptors.put(msgDescriptor.getName(), msgDescriptor);
        }
    }

    private void cacheFileDescriptors(FileDescriptorProto fileDescriptor) {
        for (DescriptorProto msgDescriptor : fileDescriptor.getMessageTypeList()) {
            descriptorCache.cache(msgDescriptor.getName(), fileDescriptor);
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ValidatorsGenPlugin.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
