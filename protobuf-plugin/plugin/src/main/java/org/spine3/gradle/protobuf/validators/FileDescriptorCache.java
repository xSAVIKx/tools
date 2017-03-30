/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.gradle.protobuf.validators;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * @author Illia Shepilov
 */
class FileDescriptorCache {

    private final Map<DescriptorProto, FileDescriptorProto> typeFiles = newHashMap();

    void cache(DescriptorProto msgDescriptor, FileDescriptorProto fileDescriptor) {
        typeFiles.put(msgDescriptor, fileDescriptor);
    }

    String getJavaPackageFor(DescriptorProto msgDescriptor) {
        final String javaPackage = typeFiles.get(msgDescriptor)
                                            .getOptions()
                                            .getJavaPackage();
        return javaPackage;
    }

    FileDescriptorProto getFileDescriptor(DescriptorProto msgDescriptor) {
        final FileDescriptorProto result = typeFiles.get(msgDescriptor);
        return result;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final FileDescriptorCache value = new FileDescriptorCache();
    }

    public static FileDescriptorCache getInstance() {
        return Singleton.INSTANCE.value;
    }
}
