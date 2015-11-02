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

package org.spine3.gradle

import groovy.util.logging.Slf4j
import org.gradle.api.*

@Slf4j
class ProtoLookupPlugin implements Plugin<Project> {

    static final String PROPERTIES_PATH_SUFFIX = "resources";
    static final String PROPERTIES_PATH_FILE_NAME = "proto_to_java_class.properties";

    @Override
    void apply(Project target) {

        final Set<Task> compileJavaTaskSet = target.getTasksByName("compileJava", false);
        final Set<Task> processResourcesTaskSet = target.getTasksByName("processResources", false);

        def compileJavaTask = null;

        if (compileJavaTaskSet.size() > 0) {
            compileJavaTask = compileJavaTaskSet.getAt(0);
        }

        final Task aTask = target.task("scanProtos", dependsOn: compileJavaTask) << {
            scanProtos(target);
        };

        if (processResourcesTaskSet.size() > 0) {
            processResourcesTaskSet.getAt(0).dependsOn(aTask)
        }
    }

    static String getProtoPropertiesFilePath(String rootDirPath) {
        return rootDirPath + "/" + PROPERTIES_PATH_SUFFIX + "/" +
                PROPERTIES_PATH_FILE_NAME;
    }

    static String replaceFileSeparatorWithDot(String filePath) {
        return filePath.replace((char) File.separatorChar, (char) '.');
    }

    static void scanProtos(Project target) {
        String targetPath = target.projectDir.absolutePath;

        log.debug "${ProtoLookupPlugin.class.getSimpleName()}: start"
        log.debug "${ProtoLookupPlugin.class.getSimpleName()}: Project path: ${targetPath}"

        String javaSuffix = ".java"
        String orBuilderSuffix = "OrBuilder" + javaSuffix

        for (String rootDirPath : ["${targetPath}/generated/main", "${targetPath}/generated/test"]) {

            log.debug "${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}"

            final String srcFolder = rootDirPath + "/java";

            File rootDir = new File(srcFolder)
            if (!rootDir.exists()) {
                log.debug "${ProtoLookupPlugin.class.getSimpleName()}: no ${rootDirPath}"
                return
            }

            File propsFileFolder = new File("${targetPath}/generated/main/" + PROPERTIES_PATH_SUFFIX)
            if (!propsFileFolder.exists()) {
                log.debug "${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: props folder does not exist"
                propsFileFolder.mkdirs();
            }
            Properties props = new Properties() {
                @Override
                public synchronized Enumeration<Object> keys() {
                    return Collections.enumeration(new TreeSet<Object>(super.keySet()));
                }
            };
            File propsFile = null;
            try {
                propsFile = new File(getProtoPropertiesFilePath(rootDirPath))
                log.debug "${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: successfully found props"
            } catch (FileNotFoundException ignored) {
            }
            if (propsFile.exists()) {
                log.debug "${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: reading properties file"

                props.load(propsFile.newDataInputStream())
                // as Properties API does not support saving default table values, we have to rewrite them all
                // Probably we should use Apache property API
                Set<String> names = props.stringPropertyNames();
                for (Iterator<String> i = names.iterator(); i.hasNext();) {
                    String propName = i.next();
                    props.setProperty(propName, props.getProperty(propName));
                }
            } else {
                log.debug "${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: creating properties file"

                propsFile.parentFile.mkdirs();
                propsFile.createNewFile();
            }
            rootDir.listFiles().each {
                log.debug "${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: reading javas"

                String prefixName = it.name
                target.fileTree(it).each {
                    if (it.name.endsWith(javaSuffix) && !it.name.endsWith(orBuilderSuffix)) {
                        log.debug "${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: found java ${it.name}"

                        String protoPath = it.path.substring((srcFolder + prefixName).length() + 2)
                        protoPath = protoPath.substring(0, protoPath.length() - javaSuffix.length())
                        protoPath = replaceFileSeparatorWithDot(protoPath)
                        String className = replaceFileSeparatorWithDot(prefixName) + "." + protoPath

                        // 'Spine3' is the abbreviation for Spine Event Engine.
                        // We have 'org.spine3' package name for Java because other 'spine' in 'org' or 'io'
                        // were occupied.
                        // We have 'spine' package for Protobuf (without '3') because it reads better.
                        String protoType = protoPath.replace("spine3", "spine")

                        props.setProperty(protoType, className)
                    }
                }
            }
            BufferedWriter writer = propsFile.newWriter();
            props.store(writer, null)
            writer.close()
            log.debug "${ProtoLookupPlugin.class.getSimpleName()}: for ${rootDirPath}: written properties"
        }
    }
}
