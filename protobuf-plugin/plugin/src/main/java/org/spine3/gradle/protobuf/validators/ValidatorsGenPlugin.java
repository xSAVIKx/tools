package org.spine3.gradle.protobuf.validators;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.gradle.SpinePlugin;
import org.spine3.gradle.protobuf.MessageTypeCache;
import org.spine3.gradle.protobuf.util.DescriptorSetUtil;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.collect.Sets.newHashSet;
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

    private final Map<String, DescriptorProto> allMessageDescriptors = newHashMap();
    private final Map<String, FileDescriptorProto> typeFiles = newHashMap();

    /** A map from Protobuf type name to Java class FQN. */
    private final MessageTypeCache messageTypeCache = new MessageTypeCache();
    private static final String JAVA_CLASS_NAME_SUFFIX = "Validator";
    private static final Pattern COMPILE = Pattern.compile(".", Pattern.LITERAL);

    @Override
    public void apply(Project project) {
        log().debug("Preparing to generate validating builders");
        final Action<Task> mainScopeAction = createAction(getMainDescriptorSetPath(project));

        logDependingTask(log(), GENERATE_VALIDATING_BUILDERS, COMPILE_JAVA, GENERATE_PROTO);
        final GradleTask generateValidator =
                newTask(GENERATE_VALIDATING_BUILDERS, mainScopeAction).insertAfterTask(GENERATE_PROTO)
                                                                      .insertBeforeTask(COMPILE_JAVA)
                                                                      .applyNowTo(project);

        log().debug("Preparing to generate test validating builders");
        final Action<Task> testScopeAction = createAction(getTestDescriptorSetPath(project));

        logDependingTask(log(), GENERATE_TEST_VALIDATING_BUILDERS, COMPILE_TEST_JAVA, GENERATE_TEST_PROTO);
        final GradleTask generateTestValidator =
                newTask(GENERATE_TEST_VALIDATING_BUILDERS, testScopeAction).insertAfterTask(GENERATE_TEST_PROTO)
                                                                           .insertBeforeTask(COMPILE_TEST_JAVA)
                                                                           .applyNowTo(project);
        log().debug("Validating builders generation phase initialized with tasks: {}, {}",
                    generateValidator,
                    generateTestValidator);

        //TODO:2017-02-28:illiashepilov: Remove when implementation will be finished.
        //test
        final String hardCodedPath = "/Users/illiashepilov/Projects/spine/tools/protobuf-plugin/build/descriptors/main.desc";
        final Set<WriterDto> dtos = process(hardCodedPath);
        for (WriterDto dto : dtos) {
            new ValidatorWriter(dto, messageTypeCache).write();
        }
        /////////////////
    }

    private Action<Task> createAction(final String path) {
        return new Action<Task>() {
            @Override
            public void execute(Task task) {
                log().debug("Generating the validators from {}", path);
                final Set<WriterDto> dtos = process(path);
                for (WriterDto dto : dtos) {
                    new ValidatorWriter(dto, messageTypeCache).write();
                }
            }
        };
    }

    private Set<WriterDto> process(String path) {
        final List<FileDescriptorProto> files = getCommandProtoFileDescriptors(path);
        final Set<WriterDto> dtos = getMessageDescriptors(files);
        final Set<WriterDto> result = getFieldDescriptors(dtos);
        return result;
    }

    private Set<WriterDto> getFieldDescriptors(Set<WriterDto> dtos) {
        final Set<WriterDto> fieldDtos = newHashSet();
        for (WriterDto dto : dtos) {
            final DescriptorProto msgDescriptor = dto.getMsgDescriptor();
            final List<FieldDescriptorProto> fieldDescriptors = msgDescriptor.getFieldList();
            final List<DescriptorProto> descriptors = processFields(fieldDescriptors);
            fieldDtos.add(dto);
            final Set<WriterDto> fieldMessages = constructMessageFieldDto(descriptors);
            fieldDtos.addAll(fieldMessages);
        }
        return fieldDtos;
    }

    private List<DescriptorProto> processFields(List<FieldDescriptorProto> fieldDescriptors) {
        final List<DescriptorProto> descriptors = newArrayList();
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
        return descriptors;
    }

    private static String getMessageName(String fullName) {
        final String[] paths = COMPILE.split(fullName);
        final String msgName = paths[paths.length - 1];
        return msgName;
    }

    private Set<WriterDto> getMessageDescriptors(List<FileDescriptorProto> files) {
        final Set<WriterDto> result = newHashSet();
        for (FileDescriptorProto file : files) {
            final List<DescriptorProto> messages = file.getMessageTypeList();
            final Set<WriterDto> dtoList = constructMessageFieldDto(messages);
            result.addAll(dtoList);
        }
        return result;
    }

    private Set<WriterDto> constructMessageFieldDto(List<DescriptorProto> messages) {
        final Set<WriterDto> result = newHashSet();
        for (DescriptorProto message : messages) {
            final WriterDto dto = createWriterDto(message);
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
            fillMap(file);
            messageTypeCache.cacheTypes(file);
            if (isCommandFile) {
                log().info("Found commands file: {}", file.getName());
                result.add(file);
            }
        }
        log().debug("Found commands in files: {}", result);

        return result;
    }

    private Map<String, FileDescriptorProto> fillMap(FileDescriptorProto fileDescriptorProto) {
        for (DescriptorProto message : fileDescriptorProto.getMessageTypeList()) {
            typeFiles.put(message.getName(), fileDescriptorProto);
        }
        return typeFiles;
    }

    private void saveMessageToMap(FileDescriptorProto file) {
        List<DescriptorProto> messages = file.getMessageTypeList();
        for (DescriptorProto msg : messages) {
            allMessageDescriptors.put(msg.getName(), msg);
        }
    }

    private WriterDto createWriterDto(DescriptorProto message) {
        final String className = message.getName() + JAVA_CLASS_NAME_SUFFIX;
        final String javaPackage = typeFiles.get(message.getName())
                                            .getOptions()
                                            .getJavaPackage();
        final WriterDto result = new WriterDto(javaPackage, className, message);
        return result;
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
