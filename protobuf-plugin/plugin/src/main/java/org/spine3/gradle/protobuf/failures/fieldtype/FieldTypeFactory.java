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
package org.spine3.gradle.protobuf.failures.fieldtype;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.squareup.javapoet.TypeName;

import java.util.AbstractMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.protobuf.DescriptorProtos.DescriptorProto;
import static org.spine3.gradle.protobuf.failures.fieldtype.ProtoPrimitives.BOOLEAN;
import static org.spine3.gradle.protobuf.failures.fieldtype.ProtoPrimitives.DOUBLE;
import static org.spine3.gradle.protobuf.failures.fieldtype.ProtoPrimitives.FLOAT;
import static org.spine3.gradle.protobuf.failures.fieldtype.ProtoPrimitives.INT;
import static org.spine3.gradle.protobuf.failures.fieldtype.ProtoPrimitives.LONG;

/**
 * Factory for creation {@link FieldType} instances.
 *
 * @author Dmytro Grankin
 */
public class FieldTypeFactory {

    /** A map from Protobuf type name to Java class FQN. */
    private final Map<String, String> messageTypeMap;
    private final Iterable<DescriptorProto> failureNestedTypes;

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
            .put(Type.TYPE_STRING.name(), "String")
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

    private static final String MAP_EXPECTED_ERROR_MESSAGE = "Map expected.";

    /**
     * Creates new instance.
     *
     * @param failureDescriptor the failure descriptor to extract nested types
     * @param messageTypeMap    pre-scanned map with proto types and their appropriate Java classes
     */
    public FieldTypeFactory(DescriptorProto failureDescriptor, Map<String, String> messageTypeMap) {
        this.messageTypeMap = messageTypeMap;
        this.failureNestedTypes = failureDescriptor.getNestedTypeList();
    }

    /**
     * Creates a {@link FieldType} instances based on {@link FieldDescriptorProto}.
     *
     * @param field the proto field descriptor
     * @return the field type
     */
    public FieldType create(FieldDescriptorProto field) {
        if (isMap(field)) {
            return new MapFieldType(getEntryTypeNames(field));
        } else {
            final String fieldTypeName = getFieldTypeName(field);

            return isRepeated(field)
                   ? new RepeatedFieldType(fieldTypeName)
                   : new SingularFieldType(fieldTypeName);
        }
    }

    private String getFieldTypeName(FieldDescriptorProto field) {
        if (field.getType() == Type.TYPE_MESSAGE
                || field.getType() == Type.TYPE_ENUM) {
            String typeName = field.getTypeName();
            // it has a redundant dot in the beginning
            if (typeName.startsWith(".")) {
                typeName = typeName.substring(1);
            }
            return messageTypeMap.get(typeName);
        } else {
            return PROTO_FIELD_TYPES.get(field.getType()
                                              .name());
        }
    }

    /**
     * Returns the key and the value type names for the map field
     * based on the passed nested types.
     *
     * @param map the field representing map
     * @return the entry containing the key and the value type names
     */
    private Map.Entry<TypeName, TypeName> getEntryTypeNames(FieldDescriptorProto map) {
        if (!isMap(map)) {
            throw new IllegalStateException(MAP_EXPECTED_ERROR_MESSAGE);
        }

        final int keyFieldIndex = 0;
        final int valueFieldIndex = 1;

        final DescriptorProto mapEntryDescriptor = getDescriptorForMapField(map);
        final TypeName keyTypeName = create(mapEntryDescriptor.getField(keyFieldIndex)).getTypeName();
        final TypeName valueTypeName = create(mapEntryDescriptor.getField(valueFieldIndex)).getTypeName();

        return new AbstractMap.SimpleEntry<>(keyTypeName, valueTypeName);
    }

    private static boolean isRepeated(FieldDescriptorProto field) {
        return field.getLabel() == FieldDescriptorProto.Label.LABEL_REPEATED;
    }

    private static boolean isMap(FieldDescriptorProto field) {
        return field.getTypeName()
                    .endsWith('.' + getEntryNameFor(field));
    }

    /**
     * Returns corresponding nested type descriptor for the map field.
     *
     * <p>Based on the fact that a message contains a nested type for
     * every map field. The nested type contains map entry description.
     *
     * @param mapField the field representing map
     * @return the nested type descriptor for map field
     */
    private DescriptorProto getDescriptorForMapField(FieldDescriptorProto mapField) {
        if (!isMap(mapField)) {
            throw new IllegalStateException(MAP_EXPECTED_ERROR_MESSAGE);
        }

        final String entryName = getEntryNameFor(mapField);
        for (DescriptorProto nestedType : failureNestedTypes) {
            if (nestedType.getName()
                          .equals(entryName)) {
                return nestedType;
            }
        }

        throw new IllegalStateException("Nested type for map field should be present.");
    }

    private static String getEntryNameFor(FieldDescriptorProto mapField) {
        final Pattern replacementPattern = Pattern.compile("_");
        final char capitalizedFirstSymbol = Character.toUpperCase(mapField.getJsonName()
                                                                          .charAt(0));
        final String remainingPart = mapField.getName()
                                             .substring(1)
                                             .replaceAll(replacementPattern.pattern(), "");

        return capitalizedFirstSymbol + remainingPart + "Entry";
    }
}
