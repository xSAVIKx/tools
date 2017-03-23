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
import org.spine3.gradle.protobuf.GenerationUtils;
import org.spine3.gradle.protobuf.MessageTypeCache;

import java.util.Collection;
import java.util.regex.Pattern;

public class ValidatingUtils {

    public static final String SETTER_PREFIX = "set";
    public static final String ADD_ALL_PREFIX = "addAll";
    public static final String PUT_ALL_PREFIX = "putAll";
    public static final String ADD_RAW_PREFIX = "addRaw";
    public static final String CREATE_IF_NEEDED = "createIfNeeded()";
    private static final Pattern PATTERN = Pattern.compile(".ChangesEntry");

    private ValidatingUtils() {
        // To prevent initialization.
    }

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
            return ClassName.get("", parameterType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static ClassName getMapValueClassName(FieldDescriptorProto fieldDescriptor,
                                                 MessageTypeCache messageTypeCache) {
        final String result = PATTERN.matcher(getParameterClass(fieldDescriptor, messageTypeCache).simpleName())
                                     .replaceAll("");
        return ClassName.get("", result);
    }

    public static ClassName getBuilderClassName(String javaPackage, String javaClass) {
        final ClassName builderClassName = ClassName.get(javaPackage, javaClass);
        return builderClassName;
    }

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
        throw new RuntimeException();
    }

    public static ClassName getStringClassName() {
        return ClassName.get("java.lang", "String");
    }
}
