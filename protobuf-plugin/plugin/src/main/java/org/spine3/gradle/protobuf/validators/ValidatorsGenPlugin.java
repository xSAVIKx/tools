package org.spine3.gradle.protobuf.validators;

import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.gradle.SpinePlugin;
import org.spine3.gradle.protobuf.util.DescriptorSetUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

    public static final String JAVA_CLASS_NAME_SUFFIX = "Validator";
    private final Map<String, DescriptorProto> allMessageDescriptors = newHashMap();
    private static final Pattern COMPILE = Pattern.compile(".", Pattern.LITERAL);
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
                final List<DescriptorProto> descriptors = process(path);

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
                log().debug("Generating the test validators from {}", path);
                final List<DescriptorProto> descriptors = process(path);

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
        final String hardCodedPath = "/Users/illiashepilov/Projects/spine/tools/protobuf-plugin/build/descriptors/main.desc";
        final List<DescriptorProto> descriptors = process(hardCodedPath);
        /////////////////
    }

    private static void createValidationBuilder(WriterDto dto) {
        new ValidatorWriter(dto).write();
    }

    private List<DescriptorProto> process(String path) {
        final List<FileDescriptorProto> files = getCommandProtoFileDescriptors(path);
        final List<WriterDto> dtos = getMessageDescriptors(files);
        final List<DescriptorProto> descriptors = newArrayList();
        for (WriterDto dto : dtos) {
            final DescriptorProto msgDescriptor = dto.getMsgDescriptor();
            descriptors.add(msgDescriptor);
            final List<FieldDescriptorProto> fieldDescriptors = msgDescriptor.getFieldList();
            processFields(descriptors, fieldDescriptors);
        }
        return descriptors;
    }

    private void processFields(List<DescriptorProto> descriptors,
                               List<FieldDescriptorProto> fieldDescriptors) {
        for (FieldDescriptorProto fieldDescriptor : fieldDescriptors) {
            final boolean isMessage = fieldDescriptor.getType() != FieldDescriptorProto.Type.TYPE_MESSAGE;
            if (isMessage) {
                continue;
            }
            final String msgName = getMessageName(fieldDescriptor.getTypeName());
            final DescriptorProto descriptor = allMessageDescriptors.get(msgName);
            if (descriptor != null) {
                descriptors.add(descriptor);
            }
        }
    }

    private static String getMessageName(String fullName) {
        final String[] paths = COMPILE.split(fullName);
        final String msgName = paths[paths.length - 1];
        return msgName;
    }

    private static List<WriterDto> getMessageDescriptors(List<FileDescriptorProto> files) {
        final List<WriterDto> result = new ArrayList<>();
        for (FileDescriptorProto file : files) {
            final List<DescriptorProto> messages = file.getMessageTypeList();
            final List<WriterDto> dtoList = constructMessageFieldDto(file, messages);
            result.addAll(dtoList);
        }
        return result;
    }

    private static List<WriterDto> constructMessageFieldDto(FileDescriptorProto file, List<DescriptorProto> messages) {
        final List<WriterDto> result = new ArrayList<>();
        for (DescriptorProto message : messages) {
            final WriterDto dto = createWriterDto(file, message);
            result.add(dto);
        }
        return result;
    }

    private List<FileDescriptorProto> getCommandProtoFileDescriptors(String descFilePath) {
        final List<FileDescriptorProto> result = new LinkedList<>();
        final DescriptorSetUtil.IsNotGoogleProto protoFilter = new DescriptorSetUtil.IsNotGoogleProto();
        final Collection<FileDescriptorProto> allDescriptors = getProtoFileDescriptors(descFilePath, protoFilter);
        for (FileDescriptorProto file : allDescriptors) {
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

    private void saveMessageToMap(FileDescriptorProto file) {
        List<DescriptorProto> messages = file.getMessageTypeList();
        for (DescriptorProto msg : messages) {
            allMessageDescriptors.put(msg.getName(), msg);
        }
    }

    private static WriterDto createWriterDto(FileDescriptorProto file, DescriptorProto message) {
        final FileOptions fileOptions = file.getOptions();
        final String packageName = fileOptions.getJavaPackage();
        final String className = message.getName() + JAVA_CLASS_NAME_SUFFIX;
        final WriterDto result = new WriterDto(packageName, className, message);
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
