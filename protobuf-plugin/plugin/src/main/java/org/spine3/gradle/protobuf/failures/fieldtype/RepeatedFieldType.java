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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.apache.commons.lang3.ClassUtils;
import org.spine3.util.Exceptions;

import java.util.Collection;

import static org.spine3.gradle.protobuf.failures.fieldtype.ProtoPrimitives.isProtoPrimitive;

/**
 * Represents repeated {@linkplain FieldType field type}.
 *
 * @author Dmytro Grankin
 */
public class RepeatedFieldType implements FieldType {

    private final TypeName typeName;

    /**
     * Constructs the {@link RepeatedFieldType} based on component type.
     *
     * @param componentTypeName the component type name
     */
    RepeatedFieldType(String componentTypeName) {
        typeName = constructTypeNameFor(componentTypeName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeName getTypeName() {
        return typeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSetterPrefix() {
        return "addAll";
    }

    private static TypeName constructTypeNameFor(String componentTypeName) {
        final TypeName componentType;

        if (isProtoPrimitive(componentTypeName)) {
            try {
                final Class<?> primitiveClass = ClassUtils.getClass(componentTypeName);
                componentType = TypeName.get(primitiveClass)
                                        .box();
            } catch (ClassNotFoundException e) {
                throw Exceptions.wrappedCause(e);
            }
        } else {
            componentType = ClassName.bestGuess(componentTypeName);
        }

        return ParameterizedTypeName.get(ClassName.get(Collection.class), componentType);
    }

    @Override
    public String toString() {
        return typeName.toString();
    }
}
