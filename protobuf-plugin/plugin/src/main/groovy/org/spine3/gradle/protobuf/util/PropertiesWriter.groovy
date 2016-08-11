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

package org.spine3.gradle.protobuf.util

import groovy.util.logging.Slf4j

import static java.util.Map.Entry

/**
 * A utility class for writing to {@code .properties} file.
 *
 * @author Alexander Litus
 */
@Slf4j
class PropertiesWriter {

    private final GString propsFilePath
    private final GString rootDirPath

    /**
     * Creates a new instance.
     *
     * @param rootDirPath a path to a directory where the {@code .properties} file is (or will be) located
     * @param propsFileName a name of the {@code .properties} file to write to (can be non-existing)
     */
    PropertiesWriter(GString rootDirPath, String propsFileName) {
        this.rootDirPath = rootDirPath
        this.propsFilePath = "$rootDirPath/$propsFileName"
    }

    /**
     * Updates the {@code .properties} file rewriting its contents if it already exists.
     *
     * @param propertiesMap a map containing properties to write to the file
     */
    void write(Map<GString, GString> propertiesMap) {
        final File rootDir = new File(rootDirPath)
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
        final Properties props = createSortedProperties()
        File file = null
        try {
            file = new File(propsFilePath)
        } catch (FileNotFoundException ignored) {
            // The file may not exist at this stage.
        }

        if (file.exists()) {
            props.load(file.newDataInputStream())
        } else {
            file.parentFile.mkdirs()
            file.createNewFile()
        }

        for (Entry<GString, GString> entry : propertiesMap.entrySet()) {
            final String key = entry.getKey().toString()
            final String value = entry.getValue().toString()
            if (!props.containsKey(key)) {
                props.setProperty(key, value)
            } else {
                final String currentValue = props.getProperty(key)
                if (!currentValue.equals(value)) {
                    log.warn("Entry with the key `$key` already exists. Value: `$currentValue`." +
                             " New value `$value` was not set.")
                }
            }
        }
        final BufferedWriter writer = file.newWriter()
        props.store(writer, /*comments=*/null)
        writer.close()
    }

    /**
     * Returns {@link Properties} instance, which has it's key set sorted by names.
     */
    private static Properties createSortedProperties() {
        final Properties props = new Properties() {
            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(new TreeSet<Object>(super.keySet()))
            }
        }
        return props
    }
}
