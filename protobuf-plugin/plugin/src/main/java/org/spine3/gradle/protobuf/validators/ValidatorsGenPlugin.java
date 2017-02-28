package org.spine3.gradle.protobuf.validators;

import com.google.protobuf.DescriptorProtos;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.gradle.SpinePlugin;
import org.spine3.gradle.protobuf.failures.MessageTypeCache;
import org.spine3.gradle.protobuf.util.DescriptorSetUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.gradle.internal.impldep.com.beust.jcommander.internal.Lists.newArrayList;
import static org.gradle.internal.impldep.com.beust.jcommander.internal.Maps.newHashMap;
import static org.spine3.gradle.TaskName.COMPILE_JAVA;
import static org.spine3.gradle.TaskName.COMPILE_TEST_JAVA;
import static org.spine3.gradle.TaskName.GENERATE_PROTO;
import static org.spine3.gradle.TaskName.GENERATE_TEST_PROTO;
import static org.spine3.gradle.TaskName.GENERATE_TEST_VALIDATING_BUILDERS;
import static org.spine3.gradle.TaskName.GENERATE_VALIDATING_BUILDERS;
import static org.spine3.gradle.protobuf.Extension.getMainDescriptorSetPath;
import static org.spine3.gradle.protobuf.Extension.getTestDescriptorSetPath;
import static org.spine3.gradle.protobuf.util.DescriptorSetUtil.getProtoFileDescriptors;

/**
 * Plugin which generates ValidationBuilders, based on commands.proto files.
 *
 * @author Illia Shepilov
 */
public class ValidatorsGenPlugin extends SpinePlugin {

    private final MessageTypeCache messageTypeCache = new MessageTypeCache();
    private final Map<String, DescriptorProtos.DescriptorProto> allMessageDescriptors = newHashMap();
    private Project project;

    @Override
    public void apply(final Project project) {
        this.project = project;

        log().debug("Preparing to generate validating builders");

        final Action<Task> mainScopeAction = new Action<Task>() {
            @Override
            public void execute(Task task) {
                final String path = getMainDescriptorSetPath(project);
                log().debug("Generating the validators from {}", path);
                final List<DescriptorProtos.FileDescriptorProto> files = getCommandProtoFileDescriptors(path);
                final List<DescriptorProtos.DescriptorProto> messages = getMessageDescriptors(files);
                for (DescriptorProtos.DescriptorProto messageDescriptor : messages) {
                    final List<DescriptorProtos.FieldDescriptorProto> fieldDescriptors = messageDescriptor.getFieldList();
                }
            }
        };

        logDependingTask(log(), GENERATE_VALIDATING_BUILDERS, COMPILE_JAVA, GENERATE_PROTO);
        final GradleTask generateValidator =
                newTask(GENERATE_VALIDATING_BUILDERS, mainScopeAction).insertAfterTask(GENERATE_PROTO)
                                                                      .insertBeforeTask(COMPILE_JAVA)
                                                                      .applyNowTo(project);

        log().debug("Preparing to generate test validating builders");
        final Action<Task> testScopeAction = new Action<Task>() {
            @Override
            public void execute(Task task) {
                final String path = getTestDescriptorSetPath(project);

                //TODO:2017-02-27:illiashepilov: finish implementation.
            }
        };

        logDependingTask(log(), GENERATE_TEST_VALIDATING_BUILDERS, COMPILE_TEST_JAVA, GENERATE_TEST_PROTO);
        final GradleTask generateTestValidator =
                newTask(GENERATE_TEST_VALIDATING_BUILDERS, testScopeAction).insertAfterTask(GENERATE_TEST_PROTO)
                                                                           .insertBeforeTask(COMPILE_TEST_JAVA)
                                                                           .applyNowTo(project);
        log().debug("Validating builders generation phase initialized with tasks: {}, {}",
                    generateValidator,
                    generateTestValidator);

        //TODO:2017-02-28:illiashepilov: Remove when implementation will be finished.
        // test
        //
        final String path = getMainDescriptorSetPath(project);
        log().debug("Generating the validators from {}", path);
        //TODO:2017-02-28:illiashepilov: Replace hardcoded path
        final List<DescriptorProtos.DescriptorProto> descriptors = newArrayList();
        final List<DescriptorProtos.FileDescriptorProto> files = getCommandProtoFileDescriptors("/Users/illiashepilov/Projects/spine/tools/protobuf-plugin/build/descriptors/main.desc");
        final List<DescriptorProtos.DescriptorProto> messages = getMessageDescriptors(files);
        for (DescriptorProtos.DescriptorProto messageDescriptor : messages) {
            descriptors.add(messageDescriptor);
            final List<DescriptorProtos.FieldDescriptorProto> fieldDescriptors = messageDescriptor.getFieldList();
            for (DescriptorProtos.FieldDescriptorProto fieldDescriptor : fieldDescriptors) {
                if(fieldDescriptor.getType() != DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE){
                   continue;
                }
                final String msgName = getMessageName(fieldDescriptor.getTypeName());
                final DescriptorProtos.DescriptorProto descriptor = allMessageDescriptors.get(msgName);
                if(descriptor!=null){
                    descriptors.add(descriptor);
                }
            }
        }
        /////////////////
    }

    private static String getMessageName(String fullName){
        final String [] paths = fullName.split(".");
        final String msgName = paths[paths.length-1];
        return msgName;
    }

    private static void getAllFieldMessages(List<DescriptorProtos.FieldDescriptorProto> fieldDescriptors) {
        for (DescriptorProtos.FieldDescriptorProto fieldDescriptor : fieldDescriptors) {
            final DescriptorProtos.FieldDescriptorProto.Type type = fieldDescriptor.getType();

        }
    }

    private static List<DescriptorProtos.DescriptorProto>
    getMessageDescriptors(List<DescriptorProtos.FileDescriptorProto> files) {
        final List<DescriptorProtos.DescriptorProto> result = new ArrayList<>();
        for (DescriptorProtos.FileDescriptorProto file : files) {
            final List<DescriptorProtos.DescriptorProto> messages = file.getMessageTypeList();
            result.addAll(messages);
        }
        return result;
    }

    private List<DescriptorProtos.FileDescriptorProto> getCommandProtoFileDescriptors(String descFilePath) {
        final List<DescriptorProtos.FileDescriptorProto> result = new LinkedList<>();
        final DescriptorSetUtil.IsNotGoogleProto protoFilter = new DescriptorSetUtil.IsNotGoogleProto();
        final Collection<DescriptorProtos.FileDescriptorProto> allDescriptors = getProtoFileDescriptors(descFilePath, protoFilter);
        for (DescriptorProtos.FileDescriptorProto file : allDescriptors) {
            final boolean isCommandFile = file.getName()
                                              .endsWith("commands.proto");
            saveMessageToMap(file);
            if (isCommandFile) {
                log().info("Found commands file: {}", file.getName());
                result.add(file);
            }
        }
        log().debug("Found commands in files: {}", result);

        return result;
    }

    private void saveMessageToMap(DescriptorProtos.FileDescriptorProto file) {
        List<DescriptorProtos.DescriptorProto> messages = file.getMessageTypeList();
        for (DescriptorProtos.DescriptorProto msg : messages) {
            allMessageDescriptors.put(msg.getName(), msg);
        }
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
