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

package org.spine3.gradle.lookup

import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.Task
import org.spine3.gradle.SubPlugin
import org.spine3.gradle.shared.SharedPreferences

@Slf4j
class ProtoLookupPlugin implements SubPlugin {

    static final String PROPERTIES_PATH_SUFFIX = "resources";
    private static final String PROPERTIES_PATH_FILE_NAME = "proto_to_java_class.properties";

    private static final String JAVA_SUFFIX = ".java";
    private static final String OR_BUILDER_SUFFIX = "OrBuilder" + JAVA_SUFFIX;

    @Override
    public void apply(Project project, SharedPreferences prefs) {

        final Task scanProtosTask = project.task("scanProtos") << {
            scanProtos(project);
        };

        scanProtosTask.dependsOn("compileJava", "generateProto", "generateTestProto");

        final Task processResources = project.getTasks().getByPath("processResources");
        processResources.dependsOn(scanProtosTask);
    }

    private static void scanProtos(Project project) {

        String projectPath = project.projectDir.absolutePath;

        log.debug("${ProtoLookupPlugin.class.getSimpleName()}: start");
        log.debug("${ProtoLookupPlugin.class.getSimpleName()}: Project path: ${projectPath}");

        for (String rootDirPath : ["${projectPath}/generated/main", "${projectPath}/generated/test"]) {

            log.debug("${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}");

            final String srcFolder = rootDirPath + "/java";

            File rootDir = new File(srcFolder);
            if (!rootDir.exists()) {
                log.debug("${ProtoLookupPlugin.class.getSimpleName()}: no ${rootDirPath}");
                continue;
            }

            File propsFileFolder = new File(rootDirPath + "/" + PROPERTIES_PATH_SUFFIX);
            if (!propsFileFolder.exists()) {
                log.debug("${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: props folder does not exist");
                propsFileFolder.mkdirs();
            }
            Properties props = new Properties() {
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
                Set<String> names = props.stringPropertyNames();
                for (Iterator<String> i = names.iterator(); i.hasNext();) {
                    String propName = i.next();
                    props.setProperty(propName, props.getProperty(propName));
                }
            } else {
                log.debug("${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: creating properties file");

                propsFile.parentFile.mkdirs();
                propsFile.createNewFile();
            }

            rootDir.listFiles().each {

                log.debug("${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: reading javas");

                String prefixName = it.name;

                project.fileTree(it).each {

                    if (it.name.endsWith(JAVA_SUFFIX) && !it.name.endsWith(OR_BUILDER_SUFFIX)) {

                        log.debug("${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: found java ${it.name}");

                        String protoPath = it.path.substring((srcFolder + prefixName).length() + 2);
                        protoPath = protoPath.substring(0, protoPath.length() - JAVA_SUFFIX.length());
                        protoPath = replaceFileSeparatorWithDot(protoPath);
                        String className = replaceFileSeparatorWithDot(prefixName) + "." + protoPath;

                        // 'Spine3' is the abbreviation for Spine Event Engine.
                        // We have 'org.spine3' package name for Java because other 'spine' in 'org' or 'io'
                        // were occupied.
                        // We have 'spine' package for Protobuf (without '3') because it reads better.
                        String protoType = protoPath.replace("spine3", "spine");

                        props.setProperty(protoType, className);
                    }
                }
            }

            BufferedWriter writer = propsFile.newWriter();
            props.store(writer, null);
            writer.close();
            log.debug("${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: written properties");
        }
    }

    private static String replaceFileSeparatorWithDot(String filePath) {
        return filePath.replace((char) File.separatorChar, (char) '.');
    }
}
