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

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.regex.Pattern;

public class GenerationUtils {

    private static final Pattern COMPILE = Pattern.compile(".", Pattern.LITERAL);

    private GenerationUtils() {
        //Prevent instantiation.
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

    public static boolean isRepeated(FieldDescriptorProto field) {
        return field.getLabel() == FieldDescriptorProto.Label.LABEL_REPEATED;
    }

    public static boolean isMap(FieldDescriptorProto field) {
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
    public static String getEntryNameFor(FieldDescriptorProto mapField) {
        final String jsonName = mapField.getJsonName();
        final char capitalizedFirstSymbol = Character.toUpperCase(jsonName.charAt(0));
        final String remainingPart = jsonName.substring(1);

        return capitalizedFirstSymbol + remainingPart + "Entry";
    }

    public static boolean isMessage(FieldDescriptorProto fieldDescriptor) {
        return fieldDescriptor.getType() == Type.TYPE_MESSAGE;
    }

    public static String getMessageName(String fullName) {
        final String[] paths = COMPILE.split(fullName);
        final String msgName = paths[paths.length - 1];
        return msgName;
    }
}
