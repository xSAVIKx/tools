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
package org.spine3.gradle.protobuf.failures;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import groovy.lang.GString;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.gradle.SpinePlugin;
import org.spine3.gradle.protobuf.util.JavaCode;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Maps.newHashMap;
import static java.io.File.separatorChar;
import static org.spine3.gradle.TaskName.COMPILE_JAVA;
import static org.spine3.gradle.TaskName.COMPILE_TEST_JAVA;
import static org.spine3.gradle.TaskName.GENERATE_FAILURES;
import static org.spine3.gradle.TaskName.GENERATE_PROTO;
import static org.spine3.gradle.TaskName.GENERATE_TEST_FAILURES;
import static org.spine3.gradle.TaskName.GENERATE_TEST_PROTO;
import static org.spine3.gradle.protobuf.Extension.getMainDescriptorSetPath;
import static org.spine3.gradle.protobuf.Extension.getTargetGenFailuresRootDir;
import static org.spine3.gradle.protobuf.Extension.getTestDescriptorSetPath;
import static org.spine3.gradle.protobuf.util.DescriptorSetUtil.getProtoFileDescriptors;

/**
 * Plugin which generates Failures, based on failures.proto files.
 *
 * <p>Uses generated proto descriptors.
 *
 * <p>Logs a warning if there are no protobuf descriptors generated.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Yevsyukov
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
public class FailuresGenPlugin extends SpinePlugin {

    private static final Pattern COMPILE = Pattern.compile(".", Pattern.LITERAL);
    private Project project;

    /** A map from Protobuf type name to Java class FQN. */
    private final Map<String, String> cachedMessageTypes = newHashMap();

    /**
     * Applies the plug-in to a project.
     *
     * <p>Adds {@code :generateFailures} and {@code :generateTestFailures} tasks.
     *
     * <p>Tasks depend on corresponding {@code :generateProto} tasks and are executed before corresponding
     * {@code :compileJava} tasks.
     */
    @Override
    public void apply(final Project project) {
        this.project = project;
        final Action<Task> mainScopeAction = new Action<Task>() {
            @Override
            public void execute(Task task) {
                final GString path = getMainDescriptorSetPath(project);
                List<FileDescriptorProto> filesWithFailures = getFailureProtoFileDescriptors(path);
                processDescriptors(filesWithFailures);
            }
        };
        final GradleTask generateFailures = newTask(GENERATE_FAILURES,
                                                    mainScopeAction).insertAfterTask(GENERATE_PROTO)
                                                                    .insertBeforeTask(COMPILE_JAVA)
                                                                    .applyNowTo(project);

        final Action<Task> testScopeAction = new Action<Task>() {
            @Override
            public void execute(Task task) {
                final GString path = getTestDescriptorSetPath(project);
                List<FileDescriptorProto> filesWithFailures = getFailureProtoFileDescriptors(path);
                processDescriptors(filesWithFailures);
            }
        };

        final GradleTask generateTestFailures = newTask(GENERATE_TEST_FAILURES,
                                                        testScopeAction).insertAfterTask(GENERATE_TEST_PROTO)
                                                                        .insertBeforeTask(COMPILE_TEST_JAVA)
                                                                        .applyNowTo(project);
        log().debug("Failure generation phase initialized with tasks: {}, {}", generateFailures, generateTestFailures);
    }

    private List<FileDescriptorProto> getFailureProtoFileDescriptors(GString descFilePath) {
        final List<FileDescriptorProto> result = new LinkedList<>();
        final Collection<FileDescriptorProto> allDescriptors = getProtoFileDescriptors(descFilePath);
        for (FileDescriptorProto file : allDescriptors) {
            if (file.getName()
                    .endsWith("failures.proto")) {
                log().info("Found failures file: {}", file.getName());
                result.add(file);
            }
            cacheTypes(file);
        }
        return result;
    }

    private void processDescriptors(List<FileDescriptorProto> descriptors) {
        for (FileDescriptorProto file : descriptors) {
            if (isFileWithFailures(file)) {
                generateFailures(file, cachedMessageTypes);
            } else {
                log().error("Invalid failures file: {}", file.getName());
            }
        }
    }

    private static boolean isFileWithFailures(FileDescriptorProto descriptor) {
        // By convention failures are generated into one file.
        if (descriptor.getOptions()
                      .getJavaMultipleFiles()) {
            return false;
        }
        final String javaOuterClassName = descriptor.getOptions()
                                                    .getJavaOuterClassname();
        if (javaOuterClassName.isEmpty()) {
            // There's no outer class name given in options.
            // Assuming the file name ends with `failures.proto`, it's a good failures file.
            return true;
        }
        final boolean result = javaOuterClassName.endsWith("Failures");
        return result;
    }

    //TODO:2016-10-07:alexander.yevsyukov: Move type loading routines into a separate class.
    private void cacheTypes(FileDescriptorProto fileDescriptor) {
        final String protoPackage = !fileDescriptor.getPackage()
                                                   .isEmpty() ? (fileDescriptor.getPackage() + '.') : "";
        String javaPackage = !fileDescriptor.getOptions()
                                            .getJavaPackage()
                                            .isEmpty() ? fileDescriptor.getOptions()
                                                                       .getJavaPackage() + '.' : "";
        if (!fileDescriptor.getOptions()
                           .getJavaMultipleFiles()) {
            final String singleFileSuffix = JavaCode.getOuterClassName(fileDescriptor);
            javaPackage = javaPackage + singleFileSuffix + '.';
        }

        for (DescriptorProto msg : fileDescriptor.getMessageTypeList()) {
            cacheMessageType(msg, protoPackage, javaPackage);
        }

        for (EnumDescriptorProto enumType : fileDescriptor.getEnumTypeList()) {
            cacheEnumType(enumType, protoPackage, javaPackage);
        }
    }

    private void cacheEnumType(EnumDescriptorProto descriptor, String protoPrefix, String javaPrefix) {
        final String key = protoPrefix + descriptor.getName();
        final String value = javaPrefix + descriptor.getName();
        cachedMessageTypes.put(key, value);
    }

    private void cacheMessageType(DescriptorProto msg, String protoPrefix, String javaPrefix) {
        final String key = protoPrefix + msg.getName();
        final String value = javaPrefix + msg.getName();
        cachedMessageTypes.put(key, value);
        if (msg.getNestedTypeCount() > 0 || msg.getEnumTypeCount() > 0) {
            final String nestedProtoPrefix = protoPrefix + msg.getName() + '.';
            final String nestedJavaPrefix = javaPrefix + msg.getName() + '.';
            for (DescriptorProto nestedMsg : msg.getNestedTypeList()) {
                cacheMessageType(nestedMsg, nestedProtoPrefix, nestedJavaPrefix);
            }
            for (EnumDescriptorProto enumType : msg.getEnumTypeList()) {
                cacheEnumType(enumType, nestedProtoPrefix, nestedJavaPrefix);
            }
        }
    }

    private void generateFailures(FileDescriptorProto descriptor, Map<String, String> messageTypeMap) {
        final GString failuresRootDir = getTargetGenFailuresRootDir(project);
        final String javaPackage = descriptor.getOptions()
                                             .getJavaPackage();
        final String javaOuterClassName = JavaCode.getOuterClassName(descriptor);
        final String packageDir = COMPILE.matcher(javaPackage)
                                         .replaceAll(Matcher.quoteReplacement("/"));
        final List<DescriptorProto> failures = descriptor.getMessageTypeList();
        for (DescriptorProto failure : failures) {
            // The name of the generated ThrowableFailure will be the same as for the Protobuf message.
            final String failureJavaPath = failuresRootDir.toString() + separatorChar
                    + packageDir + separatorChar + failure.getName() + ".java";
            final File outputFile = new File(failureJavaPath);
            final FailureWriter writer = new FailureWriter(failure, outputFile, javaPackage, javaOuterClassName, messageTypeMap);
            writer.write();
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FailuresGenPlugin.class);
    }
}
