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
import com.squareup.javapoet.AnnotationSpec;

import javax.annotation.Generated;

/**
 * Utility class for working with generation classes.
 */
public class GenerationUtils {

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

    /**
     * Checks the ProtoBuf field and determines it is repeated field or not.
     *
     * @param field the descriptor of the field to check
     * @return {@code true} if field is repeated, {@code false} otherwise
     */
    public static boolean isRepeated(FieldDescriptorProto field) {
        return field.getLabel() == FieldDescriptorProto.Label.LABEL_REPEATED;
    }

    /**
     * Checks the ProtoBuf field and determines it is map field or not.
     *
     * @param field the descriptor of the field to check
     * @return {@code true} if field is map, {@code false} otherwise
     */
    public static boolean isMap(FieldDescriptorProto field) {
        return field.getTypeName()
                    .endsWith('.' + getEntryNameFor(field));
    }

    /**
     * Checks the ProtoBuf field and determines it is message type or not.
     *
     * @param fieldDescriptor the descriptor of the field to check
     * @return {@code true} if it is message, {@code false} otherwise
     */
    public static boolean isMessage(FieldDescriptorProto fieldDescriptor) {
        return fieldDescriptor.getType() == Type.TYPE_MESSAGE;
    }

    /**
     * Removes from the ProtoBuf type name in the {@code String} representation the leading dot.
     *
     * @param typeName the type name to convert
     * @return the type name without leading dot
     */
    public static String toCorrectFieldTypeName(String typeName) {
        if (typeName.isEmpty()) {
            return typeName;
        }

        // it has a redundant dot in the beginning
        if (typeName.charAt(0) == '.') {
            return typeName.substring(1);
        }
        return typeName;
    }

    /**
     * Constructs the {@code AnnotationSpec} for the {@code Generated} class.
     *
     * @return the constructed {@code AnnotationSpec} instance
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")
    // It cannot be used as the constant across the project.
    // Although it has the equivalent literal they have the different meaning.
    public static AnnotationSpec constructGeneratedAnnotation() {
        return AnnotationSpec.builder(Generated.class)
                             .addMember("value", "$S", "by Spine compiler")
                             .build();
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
    @SuppressWarnings("DuplicateStringLiteralInspection")
    // It cannot be used as the constant across the project.
    // Although it has the equivalent literal they have the different meaning.
    public static String getEntryNameFor(FieldDescriptorProto mapField) {
        final String jsonName = mapField.getJsonName();
        final char capitalizedFirstSymbol = Character.toUpperCase(jsonName.charAt(0));
        final String remainingPart = jsonName.substring(1);

        return capitalizedFirstSymbol + remainingPart + "Entry";
    }
}
