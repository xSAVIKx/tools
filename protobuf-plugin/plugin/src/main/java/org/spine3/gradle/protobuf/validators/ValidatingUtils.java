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

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.ClassName;
import org.spine3.gradle.protobuf.MessageTypeCache;
import org.spine3.gradle.protobuf.util.GenerationUtils;

import java.util.Collection;

/**
 * Utility class for working with validator generators.
 */
public class ValidatingUtils {

    private ValidatingUtils() {
        // To prevent initialization.
    }

    /**
     *
     * @param fieldDescriptor
     * @param messageTypeCache
     * @return
     */
    public static ClassName getParameterClass(FieldDescriptorProto fieldDescriptor,
                                              MessageTypeCache messageTypeCache) {
        try {
            String typeName = fieldDescriptor.getTypeName();
            if (typeName.isEmpty()) {
                return ClassName.get(Class.forName(GenerationUtils.getType(fieldDescriptor.getType()
                                                                                          .name())));
            }
            typeName = typeName.substring(1);
            final String parameterType = messageTypeCache.getCachedTypes()
                                                         .get(typeName);
            return ClassName.bestGuess(parameterType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the {@code ClassName} according to the specified package and class.
     *
     * @param javaPackage
     * @param javaClass
     * @return
     */
    public static ClassName getClassName(String javaPackage, String javaClass) {
        final ClassName className = ClassName.get(javaPackage, javaClass);
        return className;
    }

    /**
     * Returns the {@code ClassName} for the generic parameter of the validator builder.
     *
     * @param javaPackage
     * @param messageTypeCache
     * @param descriptorName
     * @return
     */
    public static ClassName getValidatorGenericClassName(String javaPackage,
                                                         MessageTypeCache messageTypeCache,
                                                         String descriptorName) {
        final Collection<String> values = messageTypeCache.getCachedTypes()
                                                          .values();
        final String expectedClassName = javaPackage + '.' + descriptorName;
        for (String value : values) {
            if (value.equals(expectedClassName)) {
                return ClassName.get(javaPackage, descriptorName);
            }
        }
        throw new RuntimeException("Class is not found.");
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
