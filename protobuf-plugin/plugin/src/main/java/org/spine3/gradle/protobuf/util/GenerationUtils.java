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

package org.spine3.gradle.protobuf.util;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.Map;
import java.util.regex.Pattern;

import static org.spine3.gradle.protobuf.fieldtype.ProtoPrimitives.BOOLEAN;
import static org.spine3.gradle.protobuf.fieldtype.ProtoPrimitives.DOUBLE;
import static org.spine3.gradle.protobuf.fieldtype.ProtoPrimitives.FLOAT;
import static org.spine3.gradle.protobuf.fieldtype.ProtoPrimitives.INT;
import static org.spine3.gradle.protobuf.fieldtype.ProtoPrimitives.LONG;

public class GenerationUtils {

    private static final Pattern COMPILE = Pattern.compile(".", Pattern.LITERAL);

    private GenerationUtils() {
        //Prevent instantiation.
    }

    // https://developers.google.com/protocol-buffers/docs/proto3#scalar
    @SuppressWarnings({"DuplicateStringLiteralInspection", "ConstantConditions"})
    private static final Map<String, String> PROTO_FIELD_TYPES = ImmutableMap.<String, String>builder()
            .put(Type.TYPE_DOUBLE.name(), DOUBLE.getName())
            .put(Type.TYPE_FLOAT.name(), FLOAT.getName())
            .put(Type.TYPE_INT64.name(), LONG.getName())
            .put(Type.TYPE_UINT64.name(), LONG.getName())
            .put(Type.TYPE_INT32.name(), INT.getName())
            .put(Type.TYPE_FIXED64.name(), LONG.getName())
            .put(Type.TYPE_FIXED32.name(), INT.getName())
            .put(Type.TYPE_BOOL.name(), BOOLEAN.getName())
            .put(Type.TYPE_STRING.name(), "java.lang.String")
            .put(Type.TYPE_BYTES.name(), "com.google.protobuf.ByteString")
            .put(Type.TYPE_UINT32.name(), INT.getName())
            .put(Type.TYPE_SFIXED32.name(), INT.getName())
            .put(Type.TYPE_SFIXED64.name(), LONG.getName())
            .put(Type.TYPE_SINT32.name(), INT.getName())
            .put(Type.TYPE_SINT64.name(), INT.getName())

            /*
             * Groups are NOT supported, so do not create an associated Java type for it.
             * The return value for the {@link FieldDescriptorProto.Type.TYPE_GROUP} key
             * is intended to be {@code null}.
             **/
            //.put(FieldDescriptorProto.Type.TYPE_GROUP.name(), "not supported")

            .build();

    public static String getType(String type) {
        return PROTO_FIELD_TYPES.get(type);
    }

    /**
     * Transforms Protobuf-style field name into corresponding Java-style field name.
     *
     * <p>For example, seat_assignment_id -> SeatAssignmentId
     *
     * @param protoFieldName  Protobuf field name.
     * @param capitalizeFirst Indicates if we need first letter of the output to be capitalized.
     * @return a field name
     */
    public static String getJavaFieldName(String protoFieldName, boolean capitalizeFirst) {
        final String[] words = protoFieldName.split("_");
        final StringBuilder builder = new StringBuilder(words[0]);
        for (int i = 1; i < words.length; i++) {
            final String word = words[i];
            builder.append(Character.toUpperCase(word.charAt(0)))
                   .append(word.substring(1));
        }
        String resultName = builder.toString();
        if (capitalizeFirst) {
            resultName = Character.toUpperCase(resultName.charAt(0)) + resultName.substring(1);
        }
        return resultName;
    }

    public static boolean isRepeated(DescriptorProtos.FieldDescriptorProto field) {
        return field.getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED;
    }

    public static boolean isMap(DescriptorProtos.FieldDescriptorProto field) {
        return field.getTypeName()
                    .endsWith('.' + getEntryNameFor(field));
    }

    /**
     * Constructs the entry name for the map field.
     *
     * <p>For example, proto field with name 'word_dictionary' has 'wordDictionary' json name.
     * Every map field has corresponding entry type.
     * For 'word_dictionary' it would be 'WordDictionaryEntry'
     *
     * @param mapField the field to construct entry name
     * @return the name of the map field
     */
    public static String getEntryNameFor(DescriptorProtos.FieldDescriptorProto mapField) {
        final String jsonName = mapField.getJsonName();
        final char capitalizedFirstSymbol = Character.toUpperCase(jsonName.charAt(0));
        final String remainingPart = jsonName.substring(1);

        return capitalizedFirstSymbol + remainingPart + "Entry";
    }

    public static boolean isMessage(DescriptorProtos.FieldDescriptorProto fieldDescriptor) {
        return fieldDescriptor.getType() == Type.TYPE_MESSAGE;
    }

    public static String getMessageName(String fullName) {
        final String[] paths = COMPILE.split(fullName);
        final String msgName = paths[paths.length - 1];
        return msgName;
    }
}
