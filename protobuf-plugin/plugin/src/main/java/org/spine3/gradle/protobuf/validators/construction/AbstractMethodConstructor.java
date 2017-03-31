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
import org.spine3.gradle.protobuf.message.MessageTypeCache;
import org.spine3.gradle.protobuf.message.fieldtype.FieldType;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.gradle.protobuf.util.GenerationUtils.getJavaFieldName;

/**
 * An abstract base for all method constructors.
 *
 * @author Illia Shepilov
 */
abstract class AbstractMethodConstructor {

    static final String INDEX = "index";
    static final String RAW_SUFFIX = "Raw";
    static final String THIS_POINTER = "this.";
    static final String CLEAR_PREFIX = "clear";
    static final String REMOVE_PREFIX = "remove";
    static final String RETURN_THIS = "return this";
    static final String CLEAR_METHOD_CALL = ".clear()";
    static final String CALL_INITIALIZE_IF_NEEDED = "createIfNeeded()";
    static final String ADD_ALL_CONVERTED_VALUE = ".addAll(convertedValue)";
    public static final String SECOND_PARAMETER_OF_THE_VALIDATE_METHOD_CALL = ", $S)";
    private static final String PART_OF_VALIDATE_METHOD_CALL = "validate(fieldDescriptor, ";

    /**
     * Constructs the methods for the validators.
     *
     * @return the constructed methods
     */
    abstract Collection<MethodSpec> construct();

    static String createDescriptorCodeLine(int index, ClassName genericClassName) {
        final String result = "final $T fieldDescriptor = " + genericClassName +
                ".getDescriptor().getFields().get(" + index + ')';
        return result;
    }

    static String createValidateConvertedValueStatement(String valueName) {
        final String result = PART_OF_VALIDATE_METHOD_CALL + valueName +
                              SECOND_PARAMETER_OF_THE_VALIDATE_METHOD_CALL;
        return result;
    }

    static String createValidateStatement(String fileValue) {
        final String result = PART_OF_VALIDATE_METHOD_CALL + fileValue +
                              SECOND_PARAMETER_OF_THE_VALIDATE_METHOD_CALL;
        return result;
    }

    static String createGetConvertedSingularValue(String valueName) {
        final String result = "final $T converted" + getJavaFieldName(valueName, true) +
                " = getConvertedValue($T.class, " + valueName + ')';
        return result;
    }

    /**
     * An abstract base for the method constructor builders.
     */
    abstract static class AbstractMethodConstructorBuilder {

        private int fieldIndex;
        private String javaClass;
        private String javaPackage;
        private ClassName genericClassName;
        private MessageTypeCache messageTypeCache;
        private FieldDescriptorProto fieldDescriptor;
        private FieldType fieldType;

        /**
         * Builds a method constructor for the specified field.
         *
         * @return built method constructor
         */
        abstract AbstractMethodConstructor build();

        AbstractMethodConstructorBuilder setFieldIndex(int fieldIndex) {
            checkArgument(fieldIndex >= 0);
            this.fieldIndex = fieldIndex;
            return this;
        }

        AbstractMethodConstructorBuilder setJavaPackage(String javaPackage) {
            checkNotNull(javaPackage);
            this.javaPackage = javaPackage;
            return this;
        }

        AbstractMethodConstructorBuilder setJavaClass(String javaClass) {
            checkNotNull(javaClass);
            this.javaClass = javaClass;
            return this;
        }

        AbstractMethodConstructorBuilder setMessageTypeCache(MessageTypeCache messageTypeCache) {
            checkNotNull(messageTypeCache);
            this.messageTypeCache = messageTypeCache;
            return this;
        }

        AbstractMethodConstructorBuilder setFieldDescriptor(FieldDescriptorProto fieldDescriptor) {
            checkNotNull(fieldDescriptor);
            this.fieldDescriptor = fieldDescriptor;
            return this;
        }

        AbstractMethodConstructorBuilder setBuilderGenericClassName(ClassName genericClassName) {
            this.genericClassName = genericClassName;
            return this;
        }

        AbstractMethodConstructorBuilder setFieldType(FieldType fieldType) {
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

        /**
         * Checks the builder fields.
         *
         * <p>Call of that method should be used inside the {@code #build()} method
         * before the building of the method constructor.
         */
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
