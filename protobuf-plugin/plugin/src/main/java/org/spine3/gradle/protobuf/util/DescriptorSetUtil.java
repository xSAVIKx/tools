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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import groovy.lang.GString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A utility class which allows to obtain Protobuf file descriptors.
 *
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
public class DescriptorSetUtil {

    private static final String MSG_ENABLE_DESCRIPTOR_SET_GENERATION =
            "Please enable descriptor set generation. See an appropriate section at " +
                    "https://github.com/google/protobuf-gradle-plugin/" +
                    "blob/master/README.md#customize-code-generation-tasks";

    private DescriptorSetUtil() {
    }

    /**
     * Returns descriptors of all `.proto` files described in the descriptor set file.
     *
     * @param descriptorSetFilePath the path to the file generated by `protobuf-gradle-plugin`
     *                              which contains the info about project `.proto` files
     * @return a list of descriptors
     */
    @SuppressWarnings("MethodParameterNamingConvention")
    public static Collection<FileDescriptorProto> getProtoFileDescriptors(GString descriptorSetFilePath) {
        return getProtoFileDescriptors(descriptorSetFilePath, Predicates.<FileDescriptorProto>alwaysTrue());
    }

    /**
     * Returns descriptors of `.proto` files described in the descriptor set file
     * which match the filter predicate.
     *
     * @param descriptorSetFilePath the path to the file generated by `protobuf-gradle-plugin`
     *                              which contains the info about project `.proto` files
     * @param filter                a filter predicate to apply to the files
     * @return a list of descriptors
     */
    @SuppressWarnings("MethodParameterNamingConvention")
    public static Collection<FileDescriptorProto> getProtoFileDescriptors(GString descriptorSetFilePath,
                                                                          Predicate<FileDescriptorProto> filter) {
        final String filePath = descriptorSetFilePath.toString();
        if (!new File(filePath).exists()) {
            log().warn(MSG_ENABLE_DESCRIPTOR_SET_GENERATION);
            return emptyList();
        }
        final List<FileDescriptorProto> fileDescriptors = new LinkedList<>();

        try {
            final FileInputStream fis = new FileInputStream(filePath);
            final FileDescriptorSet fileDescriptorSet = FileDescriptorSet.parseFrom(fis);
            for (FileDescriptorProto file : fileDescriptorSet.getFileList()) {
                if (filter.apply(file)) {
                    fileDescriptors.add(file);
                }
            }
        } catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException e) {
            throw new RuntimeException("CAnnot get proto file descriptors. Path = " + descriptorSetFilePath, e);
        }

        return fileDescriptors;
    }

    public static class IsNotGoogleProto implements Predicate<FileDescriptorProto> {
        @Override
        public boolean apply(FileDescriptorProto file) {
            final boolean result = !file.getPackage()
                                        .contains("google");
            return result;
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(DescriptorSetUtil.class);
    }
}
