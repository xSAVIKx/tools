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

/**
 * @author Illia Shepilov
 */
abstract class MethodConstructor {

    static final String RETURN_THIS = "return this";
    static final String INDEX = "index";
    static final String VALUE = "value";
    static final String THIS_POINTER = "this.";
    static final String ADD_ALL_CONVERTED_VALUE = ".addAll(convertedValue)";

    static String createValidateConvertedValueStatement() {
        final String result = "validate(fieldDescriptor, convertedValue, $S)";
        return result;
    }

    static String createValidateStatement(String fileValue) {
        final String result = "validate(fieldDescriptor, " + fileValue + ", $S)";
        return result;
    }

    String createDescriptorCodeLine(int index, Class<?> builderGenericClass) {
        final String result = "final $T fieldDescriptor = " + builderGenericClass.getName() +
                ".getDescriptor().getFields().get(" + index + ')';
        return result;
    }

    static String createGetConvertedPluralValue() {
        final String result = "final $T<$T> convertedValue = getConvertedValue(new $T<$T<$T>>(){}.getType(), value)";
        return result;
    }

    static String createGetConvertedSingularValue() {
        final String result = "final $T convertedValue = getConvertedValue($T.class, value)";
        return result;
    }
}
