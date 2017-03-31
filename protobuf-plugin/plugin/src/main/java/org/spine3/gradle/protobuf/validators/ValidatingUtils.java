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

import com.google.common.base.Optional;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.ClassName;
import org.spine3.gradle.protobuf.message.MessageTypeCache;
import org.spine3.gradle.protobuf.util.GenerationUtils;

import java.util.Collection;

import static org.spine3.gradle.protobuf.message.fieldtype.ProtoScalarType.getBoxedScalarPrimitive;
import static org.spine3.gradle.protobuf.message.fieldtype.ProtoScalarType.getJavaTypeName;
import static org.spine3.util.Exceptions.newIllegalArgumentException;

/**
 * Utility class for working with validator generators.
 */
public class ValidatingUtils {

    private ValidatingUtils() {
        // To prevent initialization.
    }

    /**
     * Returns the {@code ClassName} for the ProtoBuf field
     * based on the passed {@code FieldDescriptorProto}.
     *
     * @param fieldDescriptor  the field descriptor of the ProtoBuf field
     * @param messageTypeCache the cache of the message types
     * @return the obtained {@code ClassName}
     */
    public static ClassName getParameterClassName(FieldDescriptorProto fieldDescriptor,
                                                  MessageTypeCache messageTypeCache) {
        String typeName = fieldDescriptor.getTypeName();
        if (typeName.isEmpty()) {
            return getJavaTypeForScalarType(fieldDescriptor);
        }
        typeName = GenerationUtils.removeLeadingDot(typeName);
        final String parameterType = messageTypeCache.getCachedTypes()
                                                     .get(typeName);
        return ClassName.bestGuess(parameterType);
    }

    private static ClassName getJavaTypeForScalarType(FieldDescriptorProto fieldDescriptor) {
        final FieldDescriptorProto.Type fieldType = fieldDescriptor.getType();
        final String scalarType = getJavaTypeName(fieldType);
        try {
            final Optional<? extends Class<?>> scalarPrimitive = getBoxedScalarPrimitive(scalarType);
            if (scalarPrimitive.isPresent()) {
                return ClassName.get(scalarPrimitive.get());
            }
            return ClassName.get(Class.forName(scalarType));
        } catch (ClassNotFoundException ex) {
            final String exMessage = String.format("Was not found the class for the type: %s",
                                                   fieldDescriptor.getType());
            throw newIllegalArgumentException(exMessage, ex);
        }
    }

    /**
     * Returns the {@code ClassName} according to the specified package and class.
     *
     * @param javaPackage the package of the class
     * @param javaClass   the name of the class
     * @return the constructed {@code ClassName}
     */
    public static ClassName getClassName(String javaPackage, String javaClass) {
        final ClassName className = ClassName.get(javaPackage, javaClass);
        return className;
    }

    /**
     * Returns the {@code ClassName} for the generic parameter of the validator builder.
     *
     * @param javaPackage      the package of the class
     * @param messageTypeCache the cache of the message types
     * @param fieldName        the name of the field
     * @return the constructed {@code ClassName}
     */
    public static ClassName getValidatorGenericClassName(String javaPackage,
                                                         MessageTypeCache messageTypeCache,
                                                         String fieldName) {
        final Collection<String> values = messageTypeCache.getCachedTypes()
                                                          .values();
        final String expectedClassName = javaPackage + '.' + fieldName;
        for (String value : values) {
            if (value.equals(expectedClassName)) {
                return ClassName.get(javaPackage, fieldName);
            }
        }
        throw newIllegalArgumentException("Class is not found.");
    }

    /**
     * Returns the {@code ClassName} for the {@code String} class.
     *
     * @return the {@code ClassName} for the {@code String} class
     */
    public static ClassName getStringClassName() {
        return ClassName.get(String.class);
    }
}
