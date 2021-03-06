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
package org.spine3.gradle.protobuf.failure.fieldtype;

import com.google.common.base.Optional;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.Collection;

/**
 * Represents repeated {@linkplain FieldType field type}.
 *
 * @author Dmytro Grankin
 */
public class RepeatedFieldType implements FieldType {

    private static final String SETTER_PREFIX = "addAll";

    private final TypeName typeName;

    /**
     * Constructs the {@link RepeatedFieldType} based on component type.
     *
     * @param componentTypeName the component type name
     */
    RepeatedFieldType(String componentTypeName) {
        this.typeName = constructTypeNameFor(componentTypeName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TypeName getTypeName() {
        return typeName;
    }

    /**
     * Returns "addAll" setter prefix,
     * used to initialize a repeated field using a protobuf message builder.
     *
     * <p>Call should be like `builder.addAllFieldName({@link java.util.List})`.
     *
     * @return {@inheritDoc}
     */
    @Override
    public String getSetterPrefix() {
        return SETTER_PREFIX;
    }

    private static TypeName constructTypeNameFor(String componentTypeName) {
        final Optional<? extends Class<?>> boxedScalarPrimitive =
                ProtoScalarType.getBoxedScalarPrimitive(componentTypeName);

        final TypeName componentType = boxedScalarPrimitive.isPresent()
                                       ? TypeName.get(boxedScalarPrimitive.get())
                                       : ClassName.bestGuess(componentTypeName);
        return ParameterizedTypeName.get(ClassName.get(Collection.class), componentType);
    }

    @Override
    public String toString() {
        return typeName.toString();
    }
}
