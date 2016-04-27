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

package org.spine3.gradle.lookup;

import groovy.util.logging.Slf4j
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plugin, which performs generated Java classes (based on protobuf) search.
 *
 * <p> Generates proto_to_java_class.properties files, which contain entries like
 * PROTO_FULL_MESSAGE_NAME=JAVA_FULL_CLASS_NAME.
 */
@Slf4j
class ProtoLookupPlugin implements Plugin<Project> {

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
            scanProtos(project);
        };

        scanProtosTask.dependsOn("compileJava", "generateProto", "generateTestProto");

        final Task processResources = project.getTasks().getByPath("processResources");
        processResources.dependsOn(scanProtosTask);
    }

    private static void scanProtos(Project project) {

        final String projectPath = project.projectDir.absolutePath;

        log.debug("${ProtoLookupPlugin.class.getSimpleName()}: start");
        log.debug("${ProtoLookupPlugin.class.getSimpleName()}: Project path: ${projectPath}");

        for (String rootDirPathSuffix : ["main", "test"]) {

            final String rootDirPath = "${projectPath}/generated/" + rootDirPathSuffix;

            log.debug("${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}");

            final String srcFolder = rootDirPath + "/java";

            final File rootDir = new File(srcFolder);
            if (!rootDir.exists()) {
                log.debug("${ProtoLookupPlugin.class.getSimpleName()}: no ${rootDirPath}");
                continue;
            }

            final File propsFileFolder = new File(rootDirPath + "/" + PROPERTIES_PATH_SUFFIX);
            if (!propsFileFolder.exists()) {
                log.debug("${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: props folder does not exist");
                propsFileFolder.mkdirs();
            }
            final Properties props = new Properties() {
                @Override
                public synchronized Enumeration<Object> keys() {
                    return Collections.enumeration(new TreeSet<Object>(super.keySet()));
                }
            };
            File propsFile = null;
            final String propsFilePath = rootDirPath + "/" + PROPERTIES_PATH_SUFFIX + "/" + PROPERTIES_PATH_FILE_NAME;
            try {
                propsFile = new File(propsFilePath);
                log.debug("${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: successfully found props");
            } catch (FileNotFoundException ignored) {
            }
            if (propsFile.exists()) {
                log.debug("${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: reading properties file");

                props.load(propsFile.newDataInputStream());
                // as Properties API does not support saving default table values, we have to rewrite them all
                // Probably we should use Apache property API
                final Set<String> names = props.stringPropertyNames();
                for (Iterator<String> i = names.iterator(); i.hasNext();) {
                    final String propName = i.next();
                    props.setProperty(propName, props.getProperty(propName));
                }
            } else {
                log.debug("${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: creating properties file");

                propsFile.parentFile.mkdirs();
                propsFile.createNewFile();
            }

            final String srcDirPath = "${projectPath}/src/" + rootDirPathSuffix;
            final String protoFilesPath = srcDirPath + "/proto";
            readProtos(props, protoFilesPath, project);

            final BufferedWriter writer = propsFile.newWriter();
            props.store(writer, null);
            writer.close();
            log.debug("${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: written properties");
        }
    }

    private static void readProtos(Properties properties, String rootProtoPath, Project project) {
        final File root = new File(rootProtoPath);
        project.fileTree(root).each {
            if (it.name.endsWith(PROTO_SUFFIX)) {
                readProtoFile(properties, it.canonicalFile);
            }
        }
    }

    private static void readProtoFile(Properties properties, File file) {
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
            // Again, won' work for }} case, move to descriptors instead of fixing
            if (trimmedLine.contains(OPENING_BRACKET)) {
                nestedClassDepth++;
            }
            if (trimmedLine.contains(CLOSING_BRACKET)) {
                nestedClassDepth--;
            }
        }

        for (String className : classes) {
            properties.setProperty(protoPackage + className, javaPackage + className);
        }
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
