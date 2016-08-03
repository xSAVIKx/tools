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

import static com.google.protobuf.DescriptorProtos.*

/**
 * A utility class which helps to get Protobuf options string which contains "unknown" option field numbers and values.
 *
 * <p>For example:
 *
 * <p>{@code 50123: "option_value"}
 *
 * <p>This is needed to get the value of an option which is "unknown" and serialized.
 *
 * <p>This can be the case if we cannot add a dependency on the artifact which contains the needed option definition.
 * For example, we should not depend on "Spine/core-java" project artifacts to avoid circular dependency.
 *
 * @author Alexander Litus
 */
@Slf4j
class ProtobufOptionsUtil {

    /** Returns a Protobuf file options string which contains option field numbers and values. */
    static String getOptionsString(FileDescriptorProto file) {
        final String optionsStr = file.getOptions().getUnknownFields().toString().trim()
        return optionsStr
    }

    /** Returns a Protobuf message options string which contains option field numbers and values. */
    static String getOptionsString(DescriptorProto message) {
        final String optionsStr = message.getOptions().getUnknownFields().toString().trim()
        return optionsStr
    }

    /** Returns a Protobuf field options string which contains option field numbers and values. */
    static String getOptionsString(FieldDescriptorProto field) {
        final String optionsStr = field.getOptions().getUnknownFields().toString().trim()
        return optionsStr
    }
}
