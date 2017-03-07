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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import org.apache.commons.lang3.ClassUtils;

import java.util.Map;

/**
 * Factory for creation {@link FieldType} instances.
 *
 * @author Dmytro Grankin
 */
public class FieldTypeFactory {

    /** A map from Protobuf type name to Java class FQN. */
    private final Map<String, String> messageTypeMap;

    // https://developers.google.com/protocol-buffers/docs/proto3#scalar
    @SuppressWarnings({"DuplicateStringLiteralInspection", "ConstantConditions"})
    private static final Map<String, String> PROTO_FIELD_TYPES = ImmutableMap.<String, String>builder()
            .put(Type.TYPE_DOUBLE.name(), "double")
            .put(Type.TYPE_FLOAT.name(), "float")
            .put(Type.TYPE_INT64.name(), "long")
            .put(Type.TYPE_UINT64.name(), "long")
            .put(Type.TYPE_INT32.name(), "int")
            .put(Type.TYPE_FIXED64.name(), "long")
            .put(Type.TYPE_FIXED32.name(), "int")
            .put(Type.TYPE_BOOL.name(), "boolean")
            .put(Type.TYPE_STRING.name(), "String")
            .put(Type.TYPE_BYTES.name(), "com.google.protobuf.ByteString")
            .put(Type.TYPE_UINT32.name(), "int")
            .put(Type.TYPE_SFIXED32.name(), "int")
            .put(Type.TYPE_SFIXED64.name(), "long")
            .put(Type.TYPE_SINT32.name(), "int")
            .put(Type.TYPE_SINT64.name(), "int")

            /*
             * Groups are NOT supported, so do not create an associated Java type for it.
             * The return value for the {@link FieldDescriptorProto.Type.TYPE_GROUP} key
             * is intended to be {@code null}.
             **/
            //.put(FieldDescriptorProto.Type.TYPE_GROUP.name(), "not supported")

            .build();

    /**
     * Creates new instance.
     *
     * @param messageTypeMap pre-scanned map with proto types and their appropriate Java classes
     */
    public FieldTypeFactory(Map<String, String> messageTypeMap) {
        this.messageTypeMap = messageTypeMap;
    }

    /**
     * Creates a {@link FieldType} instances based on {@link FieldDescriptorProto}.
     *
     * @param field the proto field descriptor
     * @return the field type
     */
    public FieldType create(FieldDescriptorProto field) {
        return create(getFieldTypeName(field), isRepeated(field));
    }

    private static FieldType create(String name, boolean repeated) {
        return repeated
               ? new RepeatedFieldType(name)
               : new SingleFieldType(name);
    }

    private String getFieldTypeName(FieldDescriptorProto field) {
        final String fieldTypeName;

        if (field.getType() == Type.TYPE_MESSAGE
                || field.getType() == Type.TYPE_ENUM) {
            String typeName = field.getTypeName();
            // it has a redundant dot in the beginning
            if (typeName.startsWith(".")) {
                typeName = typeName.substring(1);
            }
            fieldTypeName = messageTypeMap.get(typeName);
        } else {
            fieldTypeName = PROTO_FIELD_TYPES.get(field.getType()
                                                       .name());
        }

        final Optional<String> boxedTypeName = getBoxedTypeName(fieldTypeName);
        return boxedTypeName.isPresent()
               ? boxedTypeName.get()
               : fieldTypeName;
    }

    private static boolean isRepeated(FieldDescriptorProto field) {
        return field.getLabel() == FieldDescriptorProto.Label.LABEL_REPEATED;
    }

    private static Optional<String> getBoxedTypeName(String primitiveName) {
        try {
            final Class<?> primitiveClass = ClassUtils.getClass(primitiveName);
            return Optional.of(ClassUtils.primitiveToWrapper(primitiveClass)
                                         .getName());
        } catch (ClassNotFoundException e) {
            return Optional.absent();
        }
    }
}
