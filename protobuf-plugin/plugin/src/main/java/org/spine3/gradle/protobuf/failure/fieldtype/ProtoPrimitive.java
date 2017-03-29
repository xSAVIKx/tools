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

/**
 * Enumeration of the Java primitives, which
 * can be received from proto message.
 */
enum ProtoPrimitive {
    INT("int", Integer.class),
    LONG("long", Long.class),
    FLOAT("float", Float.class),
    DOUBLE("double", Double.class),
    BOOLEAN("boolean", Boolean.class);

    private final String primitiveName;
    private final Class<?> primitiveWrapper;

    ProtoPrimitive(String primitiveName, Class<?> primitiveWrapper) {
        this.primitiveName = primitiveName;
        this.primitiveWrapper = primitiveWrapper;
    }

    public String getPrimitiveName() {
        return primitiveName;
    }

    /**
     * Returns the wrapper {@link Class} for the primitive name.
     *
     * @param primitiveName the primitive name
     * @return the wrapped proto primitive class
     * or empty {@code Optional} if there are no corresponding primitive
     */
    public static Optional<? extends Class<?>> getWrappedProtoPrimitive(String primitiveName) {
        for (ProtoPrimitive primitive : ProtoPrimitive.values()) {
            if (primitiveName.equals(primitive.primitiveName)) {
                return Optional.of(primitive.primitiveWrapper);
            }
        }

        return Optional.absent();
    }
}
