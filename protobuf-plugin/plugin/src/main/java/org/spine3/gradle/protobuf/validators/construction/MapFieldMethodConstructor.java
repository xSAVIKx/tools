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

import com.google.common.reflect.TypeToken;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.ConversionException;
import org.spine3.gradle.protobuf.failure.fieldtype.MapFieldType;
import org.spine3.validate.ConstraintViolationThrowable;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.gradle.protobuf.util.GenerationUtils.getJavaFieldName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getClassName;

/**
 * A method constructor for the map fields, based on the ProtoBuf message declaration.
 *
 * @author Illia Shepilov
 */
class MapFieldMethodConstructor extends AbstractMethodConstructor {

    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String MAP_PARAM_NAME = "map";
    private static final String MAP_TO_VALIDATE_PARAM_NAME = "mapToValidate";
    private static final String MAP_TO_VALIDATE = "final $T<$T, $T> mapToValidate = ";

    private final int fieldIndex;
    private final String javaFieldName;
    private final String methodPartName;
    private final TypeName keyTypeName;
    private final TypeName valueTypeName;
    private final MapFieldType fieldType;
    private final ClassName genericClassName;
    private final ClassName builderClassName;

    private MapFieldMethodConstructor(MapFieldMethodsConstructorBuilder builder) {
        super();
        this.fieldType = (MapFieldType) builder.getFieldType();
        this.fieldIndex = builder.getFieldIndex();
        final FieldDescriptorProto fieldDescriptor = builder.getFieldDescriptor();
        this.genericClassName = builder.getGenericClassName();
        this.methodPartName = getJavaFieldName(fieldDescriptor.getName(), true);
        this.javaFieldName = getJavaFieldName(fieldDescriptor.getName(), false);
        this.builderClassName = getClassName(builder.getJavaPackage(), builder.getJavaClass());
        this.keyTypeName = fieldType.getKeyTypeName();
        this.valueTypeName = fieldType.getValueTypeName();
    }

    @Override
    Collection<MethodSpec> construct() {
        log().debug("The methods construction for the map field {} is started.", javaFieldName);
        final List<MethodSpec> methods = newArrayList();
        methods.addAll(createMapMethods());
        methods.addAll(createRawMapMethods());
        log().debug("The methods construction for the map field {} is finished.", javaFieldName);
        return methods;
    }

    private List<MethodSpec> createRawMapMethods() {
        log().debug("The raw methods construction for the map field is started.");
        final List<MethodSpec> methods = newArrayList();
        methods.add(createPutRawMethod());
        methods.add(createPutAllRawMethod());
        log().debug("The raw methods construction for the map field is finished.");
        return methods;
    }

    private List<MethodSpec> createMapMethods() {
        log().debug("The methods construction for the map field  is started.");
        final List<MethodSpec> methods = newArrayList();
        methods.add(createPutMethod());
        methods.add(createClearMethod());
        methods.add(createPutAllMethod());
        methods.add(createRemoveMethod());
        log().debug("The methods construction for the map field is finished.");
        return methods;
    }

    private MethodSpec createPutMethod() {
        final String methodName = getJavaFieldName("put" + methodPartName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, genericClassName);
        final String mapToValidate = MAP_TO_VALIDATE +
                "$T.singletonMap(" + KEY + ", " + VALUE + ')';
        final MethodSpec result =
                MethodSpec.methodBuilder(methodName)
                          .returns(builderClassName)
                          .addModifiers(Modifier.PUBLIC)
                          .addException(ConstraintViolationThrowable.class)
                          .addParameter(keyTypeName, KEY)
                          .addParameter(valueTypeName, VALUE)
                          .addStatement(CALL_INITIALIZE_IF_NEEDED)
                          .addStatement(descriptorCodeLine, FieldDescriptor.class)
                          .addStatement(mapToValidate,
                                        Map.class, keyTypeName, valueTypeName, Collections.class)
                          .addStatement(createValidateStatement(MAP_TO_VALIDATE_PARAM_NAME),
                                        javaFieldName)
                          .addStatement(THIS_POINTER + javaFieldName + ".put(key, value)")
                          .addStatement(RETURN_THIS)
                          .build();
        return result;
    }

    private MethodSpec createPutRawMethod() {
        final String methodName = getJavaFieldName("putRaw" + methodPartName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, genericClassName);
        final String mapToValidate = MAP_TO_VALIDATE +
                "$T.singletonMap(convertedKey, convertedValue)";
        final String putToTheMap = THIS_POINTER + javaFieldName +
                ".put(convertedKey, convertedValue)";
        final MethodSpec result =
                MethodSpec.methodBuilder(methodName)
                          .returns(builderClassName)
                          .addModifiers(Modifier.PUBLIC)
                          .addException(ConstraintViolationThrowable.class)
                          .addException(ConversionException.class)
                          .addParameter(String.class, KEY)
                          .addParameter(String.class, VALUE)
                          .addStatement(CALL_INITIALIZE_IF_NEEDED)
                          .addStatement(createGetConvertedSingularValue(KEY),
                                        keyTypeName, keyTypeName)
                          .addStatement(createGetConvertedSingularValue(VALUE),
                                        valueTypeName, valueTypeName)
                          .addStatement(descriptorCodeLine, FieldDescriptor.class)
                          .addStatement(mapToValidate, Map.class, keyTypeName,
                                        valueTypeName, Collections.class)
                          .addStatement(createValidateStatement(MAP_TO_VALIDATE_PARAM_NAME),
                                        javaFieldName)
                          .addStatement(putToTheMap)
                          .addStatement(RETURN_THIS)
                          .build();
        return result;
    }

    private MethodSpec createPutAllMethod() {
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, genericClassName);
        final String putAllToTheMap = THIS_POINTER + javaFieldName + ".putAll(map)";
        final MethodSpec result = MethodSpec.methodBuilder(fieldType.getSetterPrefix())
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(builderClassName)
                                            .addParameter(fieldType.getTypeName(), MAP_PARAM_NAME)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addStatement(CALL_INITIALIZE_IF_NEEDED)
                                            .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                            .addStatement(createValidateStatement(MAP_PARAM_NAME),
                                                          javaFieldName)
                                            .addStatement(putAllToTheMap)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createPutAllRawMethod() {
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, genericClassName);
        final String putAllToTheMap = THIS_POINTER + javaFieldName + ".putAll(convertedValue)";
        final MethodSpec result = MethodSpec.methodBuilder(fieldType.getSetterPrefix() + RAW_SUFFIX)
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(builderClassName)
                                            .addParameter(String.class, MAP_PARAM_NAME)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addException(ConversionException.class)
                                            .addStatement(CALL_INITIALIZE_IF_NEEDED)
                                            .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                            .addStatement(createGetConvertedMapValue(), Map.class,
                                                          keyTypeName, valueTypeName,
                                                          TypeToken.class, Map.class,
                                                          keyTypeName, valueTypeName)
                                            .addStatement(putAllToTheMap)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createRemoveMethod() {
        final String removeFromMap = THIS_POINTER + javaFieldName + ".remove(" + KEY + ')';
        final MethodSpec result = MethodSpec.methodBuilder(REMOVE_PREFIX)
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(builderClassName)
                                            .addParameter(keyTypeName, KEY)
                                            .addStatement(CALL_INITIALIZE_IF_NEEDED)
                                            .addStatement(removeFromMap)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createClearMethod() {
        final String clearMap = THIS_POINTER + javaFieldName + CLEAR_METHOD_CALL;
        final MethodSpec result = MethodSpec.methodBuilder(CLEAR_PREFIX)
                                            .addModifiers(Modifier.PUBLIC)
                                            .returns(builderClassName)
                                            .addStatement(CALL_INITIALIZE_IF_NEEDED)
                                            .addStatement(clearMap)
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private static String createGetConvertedMapValue() {
        final String result = "final $T<$T, $T> convertedValue = " +
                "getConvertedValue(new $T<$T<$T, $T>>(){}.getType(), map)";
        return result;
    }

    /**
     * Creates a new builder for the {@code MapFieldMethodsConstructor} class.
     *
     * @return created builder
     */
    static MapFieldMethodsConstructorBuilder newBuilder() {
        return new MapFieldMethodsConstructorBuilder();
    }

    /**
     * A builder for the {@code MapFieldMethodsConstructor} class.
     */
    static class MapFieldMethodsConstructorBuilder extends AbstractMethodConstructorBuilder {
        @Override
        AbstractMethodConstructor build() {
            checkFields();
            return new MapFieldMethodConstructor(this);
        }
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(MapFieldMethodConstructor.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
