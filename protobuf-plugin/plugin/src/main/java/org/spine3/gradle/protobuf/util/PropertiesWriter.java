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
package org.spine3.gradle.protobuf.util;

import groovy.lang.GString;
import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

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
        if (!rootDir.exists()) {
            final boolean result = rootDir.mkdirs();
            if (!result && !rootDir.exists() && !rootDir.isDirectory()) {
                throw new RuntimeException("Cannot create a new folder at " + rootDir.getAbsolutePath());
            }
        }
        final Properties props = createSortedProperties();
        final File file = new File(propsFilePath);

        if (file.exists()) {
            try {
                props.load(new FileInputStream(file));
            } catch (IOException e) {
                throw new RuntimeException("Error opening the file at " + file.getAbsolutePath());
            }
        } else {
            final boolean result = file.getParentFile().mkdirs();
            if (!result && !file.getParentFile().exists() && !file.getParentFile().isDirectory()) {
                throw new RuntimeException("Cannot create a new folder at " + file.getParentFile().getParentFile());
            }
        }

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

    /**
     * Returns {@link Properties} instance, which has its key set sorted by names.
     */
    private static Properties createSortedProperties() {
        return new Properties() {
            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(new TreeSet<>(super.keySet()));
            }
        };
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(GroovyPropertiesWriter.class);
    }
}
