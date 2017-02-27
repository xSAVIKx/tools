package org.spine3.gradle.protobuf.validators;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.gradle.SpinePlugin;
import org.spine3.gradle.protobuf.failures.MessageTypeCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.spine3.gradle.TaskName.COMPILE_JAVA;
import static org.spine3.gradle.TaskName.COMPILE_TEST_JAVA;
import static org.spine3.gradle.TaskName.GENERATE_FAILURES;
import static org.spine3.gradle.TaskName.GENERATE_PROTO;
import static org.spine3.gradle.TaskName.GENERATE_TEST_FAILURES;
import static org.spine3.gradle.TaskName.GENERATE_TEST_PROTO;
import static org.spine3.gradle.protobuf.Extension.getMainDescriptorSetPath;
import static org.spine3.gradle.protobuf.util.DescriptorSetUtil.getProtoFileDescriptors;

/**
 * Plugin which generates ValidationBuilders, based on commands.proto files.
 *
 * @author Illia Shepilov
 */
public class ValidatorsGenPlugin extends SpinePlugin {

    private final MessageTypeCache messageTypeCache = new MessageTypeCache();
    private Project project;

    @Override
    public void apply(Project project) {
        this.project = project;

        log().debug("Preparing to generate validating builders");

        final String path = getMainDescriptorSetPath(project);
        final Action<Task> mainScopeAction = new Action<Task>() {
            @Override
            public void execute(Task task) {
                log().debug("Generating the validators from {}", path);
                final List<DescriptorProtos.FileDescriptorProto> files = getCommandProtoFileDescriptors(path);
                final List<Descriptors.Descriptor> messages = getMessageDescriptors(files);
                for (Descriptors.Descriptor descriptor : messages) {
                    new ValidatorWriter(descriptor).write();
                }
            }
        };

        logDependingTask(log(), GENERATE_FAILURES, COMPILE_JAVA, GENERATE_PROTO);
        final GradleTask generateValidator = newTask(GENERATE_FAILURES, mainScopeAction).insertAfterTask(GENERATE_PROTO)
                                                                                        .insertBeforeTask(COMPILE_JAVA)
                                                                                        .applyNowTo(project);

        log().debug("Preparing to generate test validating builders");
        final Action<Task> testScopeAction = new Action<Task>() {
            @Override
            public void execute(Task task) {
                //TODO:2017-02-27:illiashepilov: finish implemetation.
            }
        };

        logDependingTask(log(), GENERATE_TEST_FAILURES, COMPILE_TEST_JAVA, GENERATE_TEST_PROTO);
        final GradleTask generateTestValidator =
                newTask(GENERATE_TEST_FAILURES, testScopeAction).insertAfterTask(GENERATE_TEST_PROTO)
                                                                .insertBeforeTask(COMPILE_TEST_JAVA)
                                                                .applyNowTo(project);
        log().debug("Validating builders generation phase initialized with tasks: {}, {}",
                    generateValidator,
                    generateTestValidator);
    }

    private static void generateValidators() {

    }

    private static List<Descriptors.Descriptor>
    getMessageDescriptors(List<DescriptorProtos.FileDescriptorProto> files) {
        final List<Descriptors.Descriptor> result = new ArrayList<>();
        for (DescriptorProtos.FileDescriptorProto file : files) {
            final List<DescriptorProtos.DescriptorProto> messages = file.getMessageTypeList();
            for (DescriptorProtos.DescriptorProto descriptorProto : messages) {
                result.add(descriptorProto.getDescriptorForType());
            }
        }
        return result;
    }

    private static List<DescriptorProtos.FileDescriptorProto> getCommandProtoFileDescriptors(String descFilePath) {
        final List<DescriptorProtos.FileDescriptorProto> result = new LinkedList<>();
        final Collection<DescriptorProtos.FileDescriptorProto> allDescriptors = getProtoFileDescriptors(descFilePath);
        for (DescriptorProtos.FileDescriptorProto file : allDescriptors) {
            final boolean isCommandFile = file.getName()
                                              .endsWith("commands.proto");

            if (isCommandFile) {
                log().info("Found commands file: {}", file.getName());
                result.add(file);
            }
        }
        log().debug("Found commands in files: {}", result);

        return result;
    }

    public static void main(String[] args) {

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
