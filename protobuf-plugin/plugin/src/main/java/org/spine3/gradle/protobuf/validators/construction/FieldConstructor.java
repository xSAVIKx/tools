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

package org.spine3.gradle.protobuf.validators.construction;

import com.google.protobuf.DescriptorProtos;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import org.spine3.gradle.protobuf.MessageTypeCache;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.gradle.protobuf.GenerationUtils.getJavaFieldName;
import static org.spine3.gradle.protobuf.GenerationUtils.isRepeated;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getParameterClass;

/**
 * @author Illia Shepilov
 */
public class FieldConstructor {

    private final DescriptorProtos.DescriptorProto descriptor;
    private final MessageTypeCache messageTypeCache;

    public FieldConstructor(MessageTypeCache messageTypeCache, DescriptorProtos.DescriptorProto descriptor) {
        this.messageTypeCache = messageTypeCache;
        this.descriptor = descriptor;
    }

    public Collection<FieldSpec> getAllFields() {
        final List<FieldSpec> fields = newArrayList();
        for (DescriptorProtos.FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {
            fields.add(construct(fieldDescriptor));
        }
        return fields;
    }

    private FieldSpec construct(DescriptorProtos.FieldDescriptorProto fieldDescriptor) {
        if (isRepeated(fieldDescriptor)) {
            final FieldSpec result = constructRepeatedField(fieldDescriptor);
            return result;
        }

        final FieldSpec result = constructField(fieldDescriptor);
        return result;
    }

    private FieldSpec constructRepeatedField(DescriptorProtos.FieldDescriptorProto fieldDescriptor) {
        final ClassName rawType = ClassName.get(List.class);
        final ParameterizedTypeName param = ParameterizedTypeName.get(rawType, getParameterClass(fieldDescriptor, messageTypeCache));
        final String fieldName = getJavaFieldName(fieldDescriptor.getName(), false);
        return FieldSpec.builder(param, fieldName, Modifier.PRIVATE)
                        .build();
    }

    private FieldSpec constructField(DescriptorProtos.FieldDescriptorProto fieldDescriptor) {
        final String fieldName = getJavaFieldName(fieldDescriptor.getName(), false);
        final ClassName fieldClass = getParameterClass(fieldDescriptor, messageTypeCache);
        final FieldSpec result = FieldSpec.builder(fieldClass, fieldName, Modifier.PRIVATE)
                                          .build();
        return result;
    }
}
