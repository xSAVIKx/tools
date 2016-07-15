/*
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
 */

package org.spine3.gradle.lookup.proto
import com.google.common.collect.ImmutableMap
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.spine3.gradle.util.PropertiesWriter

import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.spine3.gradle.Extension.*
/**
 * Plugin which performs generated Java classes (based on protobuf) search.
 *
 * <p>Generates a {@code .properties} file, which contains entries like:
 *
 * <p>{@code PROTO_TYPE_URL=JAVA_FULL_CLASS_NAME.}
 */
@Slf4j
class ProtoToJavaMapperPlugin implements Plugin<Project> {

    /**
     * The name of the file to populate. NOTE: also change its name used in the `core-java` project on changing.
     */
    private static final String PROPERTIES_FILE_NAME = "known_types.properties"

    private static final String PROTO_FILE_NAME_SUFFIX = ".proto"

    private static final Pattern TYPE_NAME_PATTERN = Pattern.compile("([a-zA-Z0-9]*) *\\{")
    private static final String MESSAGE_PREFIX = "message"
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("$MESSAGE_PREFIX +$TYPE_NAME_PATTERN")
    private static final String ENUM_PREFIX = "enum"
    private static final Pattern ENUM_PATTERN = Pattern.compile("$ENUM_PREFIX +$TYPE_NAME_PATTERN")

    private static final String JAVA_PACKAGE_PREFIX = "option java_package"
    private static final Pattern JAVA_PACKAGE_PATTERN = Pattern.compile(/$JAVA_PACKAGE_PREFIX *= *"(.*)";+/);

    private static final String TYPE_URL_PREFIX = "type_url_prefix"
    private static final String TYPE_URL_PREFIX_DECLARATION = "option ($TYPE_URL_PREFIX)"
    private static final Pattern TYPE_URL_PATTERN = Pattern.compile(/option \($TYPE_URL_PREFIX\) *= *"(.*)";+/);

    private static final String PROTO_PACKAGE_PREFIX = "package "
    private static final Pattern PROTO_PACKAGE_PATTERN = Pattern.compile("$PROTO_PACKAGE_PREFIX([a-zA-Z0-9_.]*);*");

    /**
     * Adds `:scanProtos` and `:scanTestProtos` tasks
     * which depend on the corresponding `:generateProto` tasks
     * and executed before the corresponding `:processResources` tasks.
     */
    @Override
    void apply(Project project) {
        final Task mapProtoToJava = project.task("mapProtoToJava") << {
            parseProtosAndWriteProps(project, getMainTargetGenResourcesDir(project), getMainProtoSrcDir(project))
        }
        mapProtoToJava.dependsOn("generateProto")
        final Task processResources = project.tasks.getByPath("processResources")
        processResources.dependsOn(mapProtoToJava)

        final Task mapTestProtoToJava = project.task("mapTestProtoToJava") << {
            parseProtosAndWriteProps(project, getTestTargetGenResourcesDir(project), getTestProtoSrcDir(project))
        }
        mapTestProtoToJava.dependsOn("generateTestProto")
        final Task processTestResources = project.tasks.getByPath("processTestResources")
        processTestResources.dependsOn(mapTestProtoToJava)
    }

    private static void parseProtosAndWriteProps(Project project,
                                                 GString targetGeneratedResourcesDir,
                                                 GString protoSrcDir) {
        final Map<GString, GString> propsMap = parseProtos(protoSrcDir, project)
        if (propsMap.isEmpty()) {
            return
        }
        final PropertiesWriter writer = new PropertiesWriter(targetGeneratedResourcesDir, PROPERTIES_FILE_NAME)
        writer.write(propsMap)
    }

    /**
     * Parses Protobuf type URLs and target Java class names from `.proto` files in the path.
     *
     * @param rootProtoPath where all `.proto` files are located
     * @param project the target project to which the task is applied
     * @return protoTypeUrl to javaClassName map
     */
    private static Map<GString, GString> parseProtos(GString rootProtoPath, Project project) {
        final ImmutableMap.Builder<GString, GString> builder = ImmutableMap.builder();
        final File root = new File(rootProtoPath)
        project.fileTree(root).each {
            if (it.name.endsWith(PROTO_FILE_NAME_SUFFIX)) {
                final Map<GString, GString> fileEntries = new ProtoParser(it.canonicalFile).parse()
                builder.putAll(fileEntries)
            }
        }
        return builder.build()
    }

    /** Parses a `.proto` file and creates a map with entries for the `.properties` file. */
    private static class ProtoParser {

        private static final String PROTO_FILE_NAME_SEPARATOR = "_"

        private static final String PROTO_FILE_NAME_SUFFIX = ".proto"

        private static final String OPENING_BRACKET = "{"
        private static final String CLOSING_BRACKET = "}"

        private static final String JAVA_MULTIPLE_FILES_OPT_PREFIX = "option java_multiple_files"
        private static final String JAVA_MULTIPLE_FILES_FALSE_VALUE = "false"
        private static final String JAVA_INNER_CLASS_SEPARATOR = "\$"

        private static final String GOOGLE_TYPE_URL_PREFIX = "type.googleapis.com"

        private final File file
        private final List<String> lines
        private GString javaPackage = GString.EMPTY
        private GString protoPackage = GString.EMPTY
        private GString commonOuterJavaClass = GString.EMPTY
        private GString typeUrlPrefix = "$GOOGLE_TYPE_URL_PREFIX"
        private final List<String> protoClasses = new ArrayList<>()
        private final List<String> javaClasses = new ArrayList<>()
        private int nestedClassDepth = 0

        private ProtoParser(File file) {
            this.file = file
            this.lines = file.readLines()
        }

        /** Parses given `.proto` file and creates a map from type URLs to Java class FQNs. */
        private ImmutableMap<GString, GString> parse() {
            for (String line : lines) {
                parseLine(line.trim())
            }
            final ImmutableMap.Builder<GString, GString> builder = ImmutableMap.builder()
            for (int i = 0; i < protoClasses.size(); i++) {
                final String protoClassName = protoClasses.get(i)
                GString javaClassName = "${javaClasses.get(i)}"
                if (!commonOuterJavaClass.isEmpty()) {
                    javaClassName = "$commonOuterJavaClass$JAVA_INNER_CLASS_SEPARATOR$javaClassName"
                }
                final GString typeUrl = "$typeUrlPrefix/$protoPackage$protoClassName"
                final GString javaClassFqn = "$javaPackage$javaClassName"
                builder.put(typeUrl, javaClassFqn)
            }
            return builder.build()
        }

        private void parseLine(String line) {
            if (line.startsWith(MESSAGE_PREFIX)) {
                addClass(parse(line, MESSAGE_PATTERN))
            } else if (line.startsWith(ENUM_PREFIX)) {
                addClass(parse(line, ENUM_PATTERN))
            } else if (line.startsWith(JAVA_PACKAGE_PREFIX) && javaPackage.isEmpty()) {
                javaPackage = parse(line, JAVA_PACKAGE_PATTERN) + "."
            } else if (line.startsWith(PROTO_PACKAGE_PREFIX) && protoPackage.isEmpty()) {
                protoPackage = parse(line, PROTO_PACKAGE_PATTERN) + "."
            } else if (line.startsWith(JAVA_MULTIPLE_FILES_OPT_PREFIX) &&
                       line.contains(JAVA_MULTIPLE_FILES_FALSE_VALUE) &&
                       commonOuterJavaClass.isEmpty()) {
                commonOuterJavaClass = toClassName(file.getName())
            } else if (line.startsWith(TYPE_URL_PREFIX_DECLARATION)) {
                typeUrlPrefix = parse(line, TYPE_URL_PATTERN)
            }
            // This won't work for bad-formatted proto files. Consider moving to descriptors.
            // Again, won't work for }} case, move to descriptors instead of fixing
            if (line.contains(OPENING_BRACKET)) {
                nestedClassDepth++
            }
            if (line.contains(CLOSING_BRACKET)) {
                nestedClassDepth--
            }
        }

        /**
         * Converts `.proto` file name to Java class name,
         * for example: `my_test.proto` to `MyTest`.
         */
        private static GString toClassName(String fullFileName) {
            final String fileName = fullFileName.substring(0, fullFileName.indexOf(PROTO_FILE_NAME_SUFFIX))
            String result = ""
            final String[] parts = fileName.split(PROTO_FILE_NAME_SEPARATOR)
            for (String part : parts) {
                final String firstChar = part.substring(0, 1).toUpperCase()
                final String partProcessed = firstChar + part.substring(1).toLowerCase()
                result += partProcessed
            }
            return "$result"
        }

        /**
         * Adds class to Proto and Java class names lists.
         *
         * <p>Responds to class depth and adds necessary parent names.
         *
         * @param className found proto class name
         */
        private void addClass(GString className) {
            GString protoFullClassName = className
            GString javaFullClassName = className
            for (int i = 0; i < nestedClassDepth; i++) {
                final int rootClassIndex = protoClasses.size() - i - 1
                protoFullClassName = "${protoClasses.get(rootClassIndex)}.$protoFullClassName"
                javaFullClassName = "${javaClasses.get(rootClassIndex)}$JAVA_INNER_CLASS_SEPARATOR$javaFullClassName"
            }
            protoClasses.add(protoFullClassName)
            javaClasses.add(javaFullClassName)
        }

        /**
         * Finds data inside the String using Pattern.
         *
         * @return matched String
         * @throws IllegalArgumentException in case of invalid data received
         */
        private GString parse(String line, Pattern pattern) {
            final Matcher matcher = pattern.matcher(line)
            if (matcher.matches()) {
                final String result = matcher.group(1)
                return "$result"
            }
            throw new IllegalArgumentException("Cannot parse: '$line' in file $file.name")
        }
    }
}
