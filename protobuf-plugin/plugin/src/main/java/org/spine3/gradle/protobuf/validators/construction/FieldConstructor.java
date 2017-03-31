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

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import org.spine3.gradle.protobuf.message.MessageTypeCache;
import org.spine3.gradle.protobuf.message.fieldtype.FieldTypeFactory;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.gradle.protobuf.util.GenerationUtils.getJavaFieldName;

/**
 * A constructor for the java fields in the generated
 * validator builders based on the ProtoBuf declaration.
 *
 * @author Illia Shepilov
 */
public class FieldConstructor {

    private final DescriptorProto descriptor;
    private final FieldTypeFactory fieldTypeFactory;

    public FieldConstructor(MessageTypeCache messageTypeCache, DescriptorProto descriptor) {
        this.descriptor = descriptor;
        this.fieldTypeFactory = new FieldTypeFactory(descriptor, messageTypeCache.getCachedTypes());
    }

    /**
     * Constructs the fields according to the {@code DescriptorProto} of the ProtoBuf message.
     *
     * @return the constructed fields
     */
    public Collection<FieldSpec> construct() {
        final List<FieldSpec> fields = newArrayList();
        for (FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {
            fields.add(construct(fieldDescriptor));
        }
        return fields;
    }

    private FieldSpec construct(FieldDescriptorProto fieldDescriptor) {
        final TypeName typeName = fieldTypeFactory.create(fieldDescriptor)
                                                  .getTypeName();
        final String fieldName = getJavaFieldName(fieldDescriptor.getName(), false);
        FieldSpec result = FieldSpec.builder(typeName, fieldName, Modifier.PRIVATE)
                                    .build();
        return result;
    }
}
