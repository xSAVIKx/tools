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

package org.spine3.gradle.lookup.proto;

import groovy.util.logging.Slf4j
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task
import org.spine3.gradle.lookup.entity.PropertiesWriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plugin, which performs generated Java classes (based on protobuf) search.
 *
 * <p> Generates proto_to_java_class.properties files, which contain entries like
 * PROTO_FULL_MESSAGE_NAME=JAVA_FULL_CLASS_NAME.
 */
@Slf4j
class ProtoToJavaMapperPlugin implements Plugin<Project> {

    static final String PROPERTIES_PATH_SUFFIX = "resources";
    private static final String PROPERTIES_PATH_FILE_NAME = "proto_to_java_class.properties";

    private static final String PROTO_SUFFIX = ".proto";

    private static final String MESSAGE_PREFIX = "message ";
    private static final String JAVA_PACKAGE_PREFIX = "option java_package";
    private static final String PROTO_PACKAGE_PREFIX = "package ";
    private static final String OPENING_BRACKET = "{";
    private static final String CLOSING_BRACKET = "}";

    private static final String NAME_REGEX = "([a-zA-Z0-9]*) *\\{";
    private static final Pattern MESSAGE_PATTERN = Pattern.compile(MESSAGE_PREFIX + NAME_REGEX);
    private static final Pattern JAVA_PACKAGE_PATTERN = Pattern.compile(JAVA_PACKAGE_PREFIX + " *= *\\\"(.*)\\\";*");
    private static final Pattern PROTO_PACKAGE_PATTERN = Pattern.compile(PROTO_PACKAGE_PREFIX + "([a-zA-Z0-9.]*);*");

    @Override
    public void apply(Project project) {

        final Task scanProtosTask = project.task("scanProtos") << {
            scanRootDir(project, "main");
        };

        final Task scanTestProtosTask = project.task("scanTestProtos") << {
            scanRootDir(project, "test");
        };

        scanProtosTask.dependsOn("generateProto");
        scanTestProtosTask.dependsOn("generateTestProto");

        final Task processResources = project.getTasks().getByPath("processResources");
        final Task processTestResources = project.getTasks().getByPath("processTestResources");

        processResources.dependsOn(scanProtosTask);
        processTestResources.dependsOn(scanTestProtosTask);
    }

    private static void scanRootDir(Project target, String rootDirPathSuffix) {
        final String projectPath = target.projectDir.absolutePath;
        final String rootDirPath = "${projectPath}/generated/${rootDirPathSuffix}";
        final String protoFilesPath = "${projectPath}/src/${rootDirPathSuffix}/proto";
        final String srcFolder = "${rootDirPath}/java";

        final File rootDir = new File(srcFolder);
        if (!rootDir.exists()) {
            log.debug("${ProtoToJavaMapperPlugin.class.getSimpleName()}: no ${rootDirPath}");
            return;
        }

        def protosPropertyValues = readProtos(protoFilesPath, target);

        final def propsFolderPath = rootDirPath + "/" + PROPERTIES_PATH_SUFFIX;
        def writer = new PropertiesWriter(propsFolderPath, PROPERTIES_PATH_FILE_NAME);
        writer.write(protosPropertyValues)
    }

    private static Map<String, String> readProtos(String rootProtoPath, Project project) {
        final def entries = new HashMap<String, String>();

        final File root = new File(rootProtoPath);
        project.fileTree(root).each {
            if (it.name.endsWith(PROTO_SUFFIX)) {
                def fileEntries = readProtoFile(it.canonicalFile);
                entries.putAll(fileEntries);
            }
        }

        return entries;
    }

    private static Map<String, String> readProtoFile(File file) {
        final List<String> lines = file.readLines();
        String javaPackage = "";
        String protoPackage = "";
        final List<String> classes = new ArrayList<>();
        int nestedClassDepth = 0; // for inner classes
        for (String line : lines) {
            def trimmedLine = line.trim();
            if (trimmedLine.startsWith(MESSAGE_PREFIX)) {
                addClass(findLineData(trimmedLine, MESSAGE_PATTERN), classes, nestedClassDepth);
            } else if (javaPackage.isEmpty() && trimmedLine.startsWith(JAVA_PACKAGE_PREFIX)) {
                javaPackage = findLineData(trimmedLine, JAVA_PACKAGE_PATTERN) + ".";
            } else if (protoPackage.isEmpty() && trimmedLine.startsWith(PROTO_PACKAGE_PREFIX)) {
                protoPackage = findLineData(trimmedLine, PROTO_PACKAGE_PATTERN) + ".";
            }
            // This won't work for bad-formatted proto files. Consider moving to descriptors.
            // Again, won't work for }} case, move to descriptors instead of fixing
            if (trimmedLine.contains(OPENING_BRACKET)) {
                nestedClassDepth++;
            }
            if (trimmedLine.contains(CLOSING_BRACKET)) {
                nestedClassDepth--;
            }
        }

        final def entries = new HashMap<String, String>();
        for (String className : classes) {
            entries.put(protoPackage + className, javaPackage + className);
        }

        return entries;
    }

    private static void addClass(String className, List<String> classes, int nestedClassDepth) {
        String fullClassName = className;
        for (int i = 0; i < nestedClassDepth; i++) {
            fullClassName = "${classes.get(classes.size() - i - 1)}.$fullClassName";
        }
        classes.add(fullClassName);
    }

    private static String findLineData(String line, Pattern pattern) {

        final Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Could not parse data: " + line);
    }
}
