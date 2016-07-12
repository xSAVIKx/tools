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

package org.spine3.gradle.lookup.proto

import groovy.util.logging.Slf4j;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.spine3.gradle.lookup.PropertiesWriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plugin which performs generated Java classes (based on Protobuf) search.
 *
 * <p>Generates a {@code .properties} file, which contains entries like:
 *
 * <p>{@code PROTO_FULL_MESSAGE_NAME=JAVA_FULL_CLASS_NAME.}
 */
@Slf4j
class ProtoToJavaMapperPlugin implements Plugin<Project> {

    private static final String PROPERTIES_PATH_SUFFIX = "resources";

    /**
     * The name of the file to populate. NOTE: also change its name used in the `core-java` project on changing.
     */
    private static final String PROPERTIES_PATH_FILE_NAME = "known_types.properties";

    private static final String PROTO_FILE__NAME_SUFFIX = ".proto";

    private static final String OPENING_BRACKET = "{";
    private static final String CLOSING_BRACKET = "}";

    private static final Pattern TYPE_NAME_PATTERN = Pattern.compile("([a-zA-Z0-9]*) *\\{");
    private static final String MESSAGE_PREFIX = "message";
    private static final Pattern MESSAGE_PATTERN = Pattern.compile(MESSAGE_PREFIX + " +" + TYPE_NAME_PATTERN);
    private static final String ENUM_PREFIX = "enum";
    private static final Pattern ENUM_PATTERN = Pattern.compile(ENUM_PREFIX + " +" + TYPE_NAME_PATTERN);

    private static final String JAVA_PACKAGE_PREFIX = "option java_package";
    private static final Pattern JAVA_PACKAGE_PATTERN = Pattern.compile(JAVA_PACKAGE_PREFIX + " *= *\\\"(.*)\\\";*");

    private static final String PROTO_PACKAGE_PREFIX = "package ";
    private static final Pattern PROTO_PACKAGE_PATTERN = Pattern.compile(PROTO_PACKAGE_PREFIX + "([a-zA-Z0-9_.]*);*");

    private static final String JAVA_MULTIPLE_FILES_OPT_PREFIX = "option java_multiple_files";
    public static final String JAVA_MULTIPLE_FILES_TRUE_VALUE = "true";
    public static final String JAVA_INNER_CLASS_SEPARATOR = "\$";

    /**
     * Applied to project.
     *
     * <p>Adds :scanProtos and :scanTestProtos tasks.
     * <p>Depends on corresponding :generateProto tasks.
     * <p>Is executed before corresponding :processResources tasks.
     * @param project
     */
    @Override
    public void apply(Project project) {
        final Task scanProtosTask = project.task("scanProtos") << {
            scanRootDir(project, "main");
        };
        scanProtosTask.dependsOn("generateProto");
        final Task scanTestProtosTask = project.task("scanTestProtos") << {
            scanRootDir(project, "test");
        };
        scanTestProtosTask.dependsOn("generateTestProto");
        final def tasks = project.getTasks()
        final Task processResources = tasks.getByPath("processResources");
        processResources.dependsOn(scanProtosTask);
        final Task processTestResources = tasks.getByPath("processTestResources");
        processTestResources.dependsOn(scanTestProtosTask);
    }

    /**
     * Scans root directory and starts reading protos and writing properties.
     *
     * @param target root directory's project.
     * @param rootDirPathSuffix source set, the task is applied to (usually, it's main or test).
     */
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

    /**
     * Extracts Proto and Java names from .proto files in path.
     *
     * @param rootProtoPath where all .proto files are located.
     * @param project target, the task is applied to.
     * @return proto-to-java names map.
     */
    private static Map<String, String> readProtos(String rootProtoPath, Project project) {
        final def entries = new HashMap<String, String>();
        final File root = new File(rootProtoPath);
        project.fileTree(root).each {
            if (it.name.endsWith(PROTO_FILE__NAME_SUFFIX)) {
                def fileEntries = readProtoFile(it.canonicalFile);
                entries.putAll(fileEntries);
            }
        }
        return entries;
    }

    /**
     * Reads Proto and Java names from a .proto file.
     *
     * @param file a .proto file.
     * @return proto-to-java names map.
     */
    private static Map<String, String> readProtoFile(File file) {
        final List<String> lines = file.readLines();
        String javaPackage = "";
        String protoPackage = "";
        String commonOuterJavaClass = "";
        final List<String> protoClasses = new ArrayList<>();
        final List<String> javaClasses = new ArrayList<>();
        int nestedClassDepth = 0; // for inner protoClasses
        for (String line : lines) {
            final String trimmedLine = line.trim();
            if (trimmedLine.startsWith(MESSAGE_PREFIX)) {
                addClass(findLineData(trimmedLine, MESSAGE_PATTERN), protoClasses, javaClasses, nestedClassDepth);
            } else if (trimmedLine.startsWith(ENUM_PREFIX)) {
                addClass(findLineData(trimmedLine, ENUM_PATTERN), protoClasses, javaClasses, nestedClassDepth);
            } else if (trimmedLine.startsWith(JAVA_PACKAGE_PREFIX) && javaPackage.isEmpty()) {
                javaPackage = findLineData(trimmedLine, JAVA_PACKAGE_PATTERN) + ".";
            } else if (trimmedLine.startsWith(PROTO_PACKAGE_PREFIX) && protoPackage.isEmpty()) {
                protoPackage = findLineData(trimmedLine, PROTO_PACKAGE_PATTERN) + ".";
            } else if (trimmedLine.startsWith(JAVA_MULTIPLE_FILES_OPT_PREFIX) && commonOuterJavaClass.isEmpty()) {
                commonOuterJavaClass = getCommonOuterJavaClass(trimmedLine, file);
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
        final Map entries = new HashMap<String, String>();
        for (int i = 0; i < protoClasses.size(); i++) {
            final String protoClassName = protoClasses.get(i);
            String javaClassName = javaClasses.get(i);
            if (!commonOuterJavaClass.isEmpty()) {
                javaClassName = "$commonOuterJavaClass$JAVA_INNER_CLASS_SEPARATOR$javaClassName";
            }
            entries.put(protoPackage + protoClassName, javaPackage + javaClassName);
        }
        return entries;
    }

    /**
     * Retrieves the name of outer Java class for proto file, if the file has java_multiple_files attribute.
     *
     * @param trimmedLine the line to scan for the desired attribute.
     * @param file the file to scan.
     * @return empty String in case of no value found, Java class name otherwise.
     */
    private static String getCommonOuterJavaClass(String trimmedLine, File file) {
        boolean isMultipleFiles = trimmedLine.contains(JAVA_MULTIPLE_FILES_TRUE_VALUE);
        if (!isMultipleFiles) {
            final String fileNameFull = file.getName();
            final String fileName = fileNameFull.substring(0, fileNameFull.indexOf(PROTO_FILE__NAME_SUFFIX));
            final String firstChar = fileName.substring(0, 1).toUpperCase();
            final String result = firstChar + fileName.substring(1);
            return result;
        }
        return "";
    }

    /**
     * Adds class to Proto and Java class names lists.
     *
     * <p>Responds to class depth and adds necessary parent names.
     *
     * @param className Found proto class name.
     * @param protoClasses List with fully qualified proto class names.
     * @param javaClasses List with fully qualified java class names.
     * @param nestedClassDepth Nested depth to determine the fully qualified name.
     */
    private static void addClass(String className,
                                 List<String> protoClasses,
                                 List<String> javaClasses,
                                 int nestedClassDepth) {
        String protoFullClassName = className;
        String javaFullClassName = className;
        for (int i = 0; i < nestedClassDepth; i++) {
            final int rootClassIndex = protoClasses.size() - i - 1;
            protoFullClassName = "${protoClasses.get(rootClassIndex)}.$protoFullClassName";
            javaFullClassName = "${javaClasses.get(rootClassIndex)}$JAVA_INNER_CLASS_SEPARATOR$javaFullClassName";
        }
        protoClasses.add(protoFullClassName);
        javaClasses.add(javaFullClassName);
    }

    /**
     * Finds data inside the String using Pattern.
     * @return matched String.
     * @throws IllegalArgumentException in case of invalid data received.
     */
    private static String findLineData(String line, Pattern pattern) {
        final Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Could not parse data: " + line);
    }
}
