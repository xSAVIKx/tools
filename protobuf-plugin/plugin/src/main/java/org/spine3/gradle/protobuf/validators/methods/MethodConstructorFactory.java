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

package org.spine3.gradle.protobuf.validators.methods;

import com.google.protobuf.DescriptorProtos;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import org.spine3.gradle.protobuf.MessageTypeCache;
import org.spine3.gradle.protobuf.validators.WriterDto;

import javax.lang.model.element.Modifier;

import static org.spine3.gradle.protobuf.GenerationUtils.getJavaFieldName;
import static org.spine3.gradle.protobuf.GenerationUtils.isRepeated;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.ADD_ALL_PREFIX;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.SETTER_PREFIX;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getBuilderClassName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getValidatingBuilderGenericClassName;

/**
 * @author Illia Shepilov
 */
public class MethodConstructorFactory {

    private final String javaClass;
    private final String javaPackage;
    private final ClassName builderGenericClassName;
    private final MessageTypeCache messageTypeCache;
    private final DescriptorProtos.DescriptorProto descriptor;

    public MethodConstructorFactory(WriterDto writerDto, MessageTypeCache messageTypeCache) {
        this.javaClass = writerDto.getJavaClass();
        this.javaPackage = writerDto.getJavaPackage();
        this.descriptor = writerDto.getMsgDescriptor();
        this.messageTypeCache = messageTypeCache;
        builderGenericClassName = getValidatingBuilderGenericClassName(javaPackage,
                                                                       messageTypeCache,
                                                                       descriptor.getName());
    }

    public static MethodSpec createPrivateConstructor() {
        final MethodSpec result = MethodSpec.constructorBuilder()
                                            .addModifiers(Modifier.PRIVATE)
                                            .build();
        return result;
    }

    public MethodSpec createNewBuilderMethod() {
        final ClassName builderClass = getBuilderClassName(javaPackage, javaClass);
        final MethodSpec buildMethod = MethodSpec.methodBuilder("newBuilder")
                                                 .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                                 .returns(builderClass)
                                                 .addStatement("return new $T()", builderClass)
                                                 .build();
        return buildMethod;
    }

    public MethodSpec createBuildMethod() {
        final StringBuilder builder = new StringBuilder("final $T result = $T.newBuilder()");
        for (DescriptorProtos.FieldDescriptorProto fieldDescriptor : descriptor.getFieldList()) {
            builder.append('.');

            if (isRepeated(fieldDescriptor)) {
                builder.append(ADD_ALL_PREFIX);
            } else {
                builder.append(SETTER_PREFIX);
            }

            builder.append(getJavaFieldName(fieldDescriptor.getName(), true))
                   .append('(')
                   .append(getJavaFieldName(fieldDescriptor.getName(), false))
                   .append(')');
        }
        builder.append(".build()");

        final MethodSpec buildMethod = MethodSpec.methodBuilder("build")
                                                 .addModifiers(Modifier.PUBLIC)
                                                 .returns(builderGenericClassName)
                                                 .addStatement(builder.toString(),
                                                               builderGenericClassName,
                                                               builderGenericClassName)
                                                 .addStatement("return result")
                                                 .build();
        return buildMethod;
    }
}
