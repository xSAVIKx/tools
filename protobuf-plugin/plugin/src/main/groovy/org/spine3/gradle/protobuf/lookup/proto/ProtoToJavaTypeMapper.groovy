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

package org.spine3.gradle.protobuf.lookup.proto
import com.google.common.base.Joiner
import com.google.common.collect.ImmutableMap
import groovy.util.logging.Slf4j

import static com.google.common.collect.Lists.newLinkedList
import static com.google.protobuf.DescriptorProtos.*
import static org.spine3.gradle.protobuf.util.ProtobufOptionsUtil.getUnknownOptionValue

/**
 * Maps Protobuf message types from a file to the corresponding Java classes.
 */
@Slf4j
class ProtoToJavaTypeMapper {

    /**
     * The field number of the file option `type_url_prefix` defined in the `Spine/core-java` project.
     */
    private static final Long OPTION_NUMBER_TYPE_URL_PREFIX = 58204L

    private static final String JAVA_INNER_CLASS_SEPARATOR = "\$"

    // TODO:2016-08-03:alexander.litus: DRY
    /** A separator used in Protobuf type names and Java packages. */
    private static final String DOT = "."

    private static final String GOOGLE_TYPE_URL_PREFIX = "type.googleapis.com"
    private static final String PROTO_TYPE_URL_SEPARATOR = "/"

    private static final String PROTO_FILE_NAME_SEPARATOR = "_"

    private final FileDescriptorProto file

    private final String protoPackagePrefix
    private final String javaPackagePrefix
    private final String typeUrlPrefix
    private final String commonOuterClassPrefix

    ProtoToJavaTypeMapper(FileDescriptorProto file) {
        this.file = file
        this.protoPackagePrefix = getProtoPackagePrefix(file)
        this.javaPackagePrefix = getJavaPackagePrefix(file)
        this.typeUrlPrefix = getTypeUrlPrefix(file)
        this.commonOuterClassPrefix = getCommonOuterJavaClassPrefix(file)
    }

    // TODO:2016-08-04:alexander.litus: enums!

    /** Returns a map from Protobuf type url to the corresponding fully-qualified Java class name. */
    Map<GString, GString> mapTypes() {
        final ImmutableMap.Builder<GString, GString> builder = ImmutableMap.builder()
        final List<DescriptorProto> messages = file.messageTypeList
        putEntries(builder, messages, newLinkedList())
        return builder.build()
    }

    private void putEntries(ImmutableMap.Builder<GString, GString> builder,
                            Iterable<DescriptorProto> messages,
                            Collection<String> parentMsgNames) {
        for (DescriptorProto msg : messages) {
            putEntry(builder, msg, parentMsgNames)
        }
    }

    private void putEntry(ImmutableMap.Builder<GString, GString> builder,
                          DescriptorProto msg,
                          Collection<String> parentMsgNames) {
        final String typeUrl = getTypeUrl(msg, parentMsgNames)
        final String javaClassName = getJavaClassName(msg, parentMsgNames)
        builder.put("$typeUrl", "$javaClassName")
        final List<DescriptorProto> nestedTypes = msg.nestedTypeList
        if (!nestedTypes.isEmpty()) {
            parentMsgNames.add(msg.name)
            putEntries(builder, nestedTypes, parentMsgNames)
        }
    }

    private String getTypeUrl(DescriptorProto msg, Collection<String> parentMsgNames) {
        final String parentMessagesPrefix = getParentTypesPrefix(parentMsgNames, DOT)
        final String result = typeUrlPrefix + protoPackagePrefix + parentMessagesPrefix + msg.name
        return result
    }

    private String getJavaClassName(DescriptorProto msg, Collection<String> parentTypeNames) {
        final String parentClassesPrefix = getParentTypesPrefix(parentTypeNames, JAVA_INNER_CLASS_SEPARATOR)
        final String result = javaPackagePrefix + commonOuterClassPrefix + parentClassesPrefix + msg.name
        return result
    }

    private static String getProtoPackagePrefix(FileDescriptorProto file) {
        final String result = file.package.trim() ? (file.package + DOT) : ""
        return result
    }

    private static String getJavaPackagePrefix(FileDescriptorProto file) {
        final String pack = file.options.javaPackage
        final String result = pack.trim() ? (pack + DOT) : ""
        return result
    }

    private static String getTypeUrlPrefix(FileDescriptorProto file) {
        final String prefix = getUnknownOptionValue(file, OPTION_NUMBER_TYPE_URL_PREFIX)
        final String result = prefix ?
                (prefix + PROTO_TYPE_URL_SEPARATOR) :
                (GOOGLE_TYPE_URL_PREFIX + PROTO_TYPE_URL_SEPARATOR)
        return result
    }

    private static String getCommonOuterJavaClassPrefix(FileDescriptorProto file) {
        String commonOuterClass = ""
        final FileOptions options = file.options
        if (!options.javaMultipleFiles) {
            commonOuterClass = options.javaOuterClassname ? options.javaOuterClassname : toClassName(file.name)
        }
        final String result = commonOuterClass ? (commonOuterClass + JAVA_INNER_CLASS_SEPARATOR) : ""
        return result
    }

    private static String getParentTypesPrefix(Collection<String> parentTypeNames, String separator) {
        if (parentTypeNames.isEmpty()) {
            return ""
        }
        final String result = Joiner.on(separator).join(parentTypeNames) + separator
        return result
    }

    /**
     * Converts `.proto` file name to Java class name,
     * for example: `my_test.proto` to `MyTest`.
     */
    private static String toClassName(String fullFileName) {
        final String fileName = fullFileName.substring(fullFileName.lastIndexOf("/") + 1, fullFileName.lastIndexOf(DOT))
        StringBuilder builder = new StringBuilder(128)
        final String[] parts = fileName.split(PROTO_FILE_NAME_SEPARATOR)
        for (String part : parts) {
            final String firstChar = part.substring(0, 1).toUpperCase()
            final String partProcessed = firstChar + part.substring(1).toLowerCase()
            builder.append(partProcessed)
        }
        return builder.toString()
    }
}
