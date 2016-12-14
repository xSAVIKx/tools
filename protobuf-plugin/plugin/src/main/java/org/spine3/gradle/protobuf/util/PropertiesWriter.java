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
package org.spine3.gradle.protobuf.util;

import com.google.common.io.Files;
import groovy.lang.GString;
import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

/**
 * A utility class for writing to {@code .properties} file.
 *
 * @author Alexander Litus, Alex Tymchenko
 */
@Slf4j
public class PropertiesWriter {

    private final String propsFilePath;
    private final String rootDirPath;

    /**
     * Creates a new instance.
     *
     * @param rootDirPath   a path to a directory where the {@code .properties} file is (or will be) located
     * @param propsFileName a name of the {@code .properties} file to write to (can be non-existing)
     */
    public PropertiesWriter(GString rootDirPath, String propsFileName) {
        this.rootDirPath = rootDirPath.toString();
        this.propsFilePath = rootDirPath + File.separator + propsFileName;
    }

    /**
     * Updates the {@code .properties} file rewriting its contents if it already exists.
     *
     * @param propertiesMap a map containing properties to write to the file
     */
    public void write(Map<String, String> propertiesMap) {
        final File rootDir = new File(rootDirPath);
        createParentFolders(rootDir);

        final Properties props = new SortedProperties();
        final File file = new File(propsFilePath);
        prepareTargetFile(props, file);

        for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (!props.containsKey(key)) {
                props.setProperty(key, value);
            } else {
                final String currentValue = props.getProperty(key);
                if (!currentValue.equals(value)) {
                    log().warn("Entry with the key `%s` already exists. Value: `%s`." +
                                       " New value `%s` was not set.", key, currentValue, value);
                }
            }
        }

        try {
            final FileWriter outFileWriter = new FileWriter(file);
            final BufferedWriter bufferedWriter = new BufferedWriter(outFileWriter);
            props.store(bufferedWriter, /*comments=*/null);
            bufferedWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void prepareTargetFile(Properties props, File file) {
        if (file.exists()) {
            try {
                final FileInputStream fis = new FileInputStream(file);
                props.load(fis);
            } catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException e) {
                throw new RuntimeException("Error loading the properties from the file: " + file.getAbsolutePath(), e);
            }
        } else {
            createParentFolders(file);
        }
    }

    private static void createParentFolders(File file) {
        try {
            Files.createParentDirs(file);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create parent folders at " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Customized {@link Properties}, which key set is sorted.
     *
     * <p>The instance of this class is used to maintain the alphanumerical order in the {@code .properties} files
     * generated out of the instance contents.
     *
     * <p>Such a trick simplifies the resulting {@code .properties} file navigation
     * and makes any potential debugging easier.
     */
    @SuppressWarnings("ClassExtendsConcreteCollection")     // It's the best (and still readable) way for customization.
    private static final class SortedProperties extends Properties {

        // Generated automatically.
        private static final long serialVersionUID = -4508611340425795981L;

        @SuppressWarnings("RefusedBequest")     // as we replace `keys()` with a completely different behavior.
        @Override
        public synchronized Enumeration<Object> keys() {
            return Collections.enumeration(new TreeSet<>(keySet()));
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(PropertiesWriter.class);
    }
}