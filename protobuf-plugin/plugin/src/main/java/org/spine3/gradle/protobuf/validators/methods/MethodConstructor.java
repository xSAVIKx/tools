package org.spine3.gradle.protobuf.validators.methods;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;

/**
 * @author Illia Shepilov
 */
abstract class MethodConstructor {

    static final String RETURN_THIS = "return this";
    static final String INDEX = "index";
    static final String VALUE = "value";
    static final String THIS_POINTER = "this.";
    static final String ADD_ALL_CONVERTED_VALUE = ".addAll(convertedValue)";
    static final String SETTER_PREFIX = "set";
    static final String ADD_ALL_PREFIX = "addAll";

    static String createValidateConvertedValueStatement() {
        final String result = "validate(fieldDescriptor, convertedValue, $S)";
        return result;
    }

    static String createValidateStatement(String fileValue) {
        final String result = "validate(fieldDescriptor, " + fileValue + ", $S)";
        return result;
    }

    String createDescriptorCodeLine(int index, FieldDescriptorProto fieldDescriptor) {
        final String result = "final $T fieldDescriptor = " + builderGenericClass.getName() +
                ".getDescriptor().getFields().get(" + index + ')';
        return result;
    }

    static String createGetConvertedPluralValue() {
        final String result = "final $T<$T> convertedValue = getConvertedValue(new $T<>($T.class, $T.class), value)";
        return result;
    }

    static String createGetConvertedSingularValue() {
        final String result = "final $T convertedValue = getConvertedValue(new $T<>($T.class), value)";
        return result;
    }
}
