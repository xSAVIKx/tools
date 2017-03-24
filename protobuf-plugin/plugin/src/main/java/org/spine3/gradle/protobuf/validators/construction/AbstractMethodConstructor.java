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

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import org.spine3.gradle.protobuf.MessageTypeCache;
import org.spine3.gradle.protobuf.fieldtype.FieldType;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Illia Shepilov
 */
abstract class AbstractMethodConstructor {

    static final String RETURN_THIS = "return this";
    static final String INDEX = "index";
    static final String VALUE = "value";
    static final String THIS_POINTER = "this.";
    static final String ADD_ALL_CONVERTED_VALUE = ".addAll(convertedValue)";

    abstract Collection<MethodSpec> construct();

    String createDescriptorCodeLine(int index, ClassName genericClassName) {
        final String result = "final $T fieldDescriptor = " + genericClassName +
                ".getDescriptor().getFields().get(" + index + ')';
        return result;
    }

    static String createValidateConvertedValueStatement() {
        final String result = "validate(fieldDescriptor, convertedValue, $S)";
        return result;
    }

    static String createValidateStatement(String fileValue) {
        final String result = "validate(fieldDescriptor, " + fileValue + ", $S)";
        return result;
    }

    static String createGetConvertedPluralValue() {
        final String result = "final $T<$T> convertedValue = " +
                "getConvertedValue(new $T<$T<$T>>(){}.getType(), value)";
        return result;
    }

    static String createGetConvertedSingularValue() {
        final String result = "final $T convertedValue = getConvertedValue($T.class, value)";
        return result;
    }

    static String createGetConvertedMapValue(){
        final String result = "final $T<$T, $T> convertedValue = " +
                "getConvertedValue(new $T<$T<$T, $T>>(){}.getType(), value)";
        return result;
    }

    abstract static class MethodConstructorBuilder {

        private int fieldIndex;
        private String javaClass;
        private String javaPackage;
        private ClassName genericClassName;
        private MessageTypeCache messageTypeCache;
        private FieldDescriptorProto fieldDescriptor;
        private FieldType fieldType;

        abstract AbstractMethodConstructor build();

        MethodConstructorBuilder setFieldIndex(int fieldIndex) {
            checkArgument(fieldIndex >= 0);
            this.fieldIndex = fieldIndex;
            return this;
        }

        MethodConstructorBuilder setJavaPackage(String javaPackage) {
            checkNotNull(javaPackage);
            this.javaPackage = javaPackage;
            return this;
        }

        MethodConstructorBuilder setJavaClass(String javaClass) {
            checkNotNull(javaClass);
            this.javaClass = javaClass;
            return this;
        }

        MethodConstructorBuilder setMessageTypeCache(MessageTypeCache messageTypeCache) {
            checkNotNull(messageTypeCache);
            this.messageTypeCache = messageTypeCache;
            return this;
        }

        MethodConstructorBuilder setFieldDescriptor(FieldDescriptorProto fieldDescriptor) {
            checkNotNull(fieldDescriptor);
            this.fieldDescriptor = fieldDescriptor;
            return this;
        }

        MethodConstructorBuilder setBuilderGenericClassName(ClassName genericClassName) {
            this.genericClassName = genericClassName;
            return this;
        }

        MethodConstructorBuilder setFieldType(FieldType fieldType) {
            this.fieldType = fieldType;
            return this;
        }

        int getFieldIndex() {
            return fieldIndex;
        }

        String getJavaClass() {
            return javaClass;
        }

        String getJavaPackage() {
            return javaPackage;
        }

        ClassName getGenericClassName() {
            return genericClassName;
        }

        MessageTypeCache getMessageTypeCache() {
            return messageTypeCache;
        }

        FieldDescriptorProto getFieldDescriptor() {
            return fieldDescriptor;
        }

        public FieldType getFieldType() {
            return fieldType;
        }

        void checkFields() {
            checkNotNull(javaClass);
            checkNotNull(javaPackage);
            checkNotNull(messageTypeCache);
            checkNotNull(fieldDescriptor);
            checkNotNull(genericClassName);
            checkNotNull(fieldType);
            checkArgument(fieldIndex >= 0);
        }
    }
}
