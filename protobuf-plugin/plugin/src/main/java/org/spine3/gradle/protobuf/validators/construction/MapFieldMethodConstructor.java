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
import org.spine3.base.ConversionException;
import org.spine3.gradle.protobuf.fieldtype.MapFieldType;
import org.spine3.validate.ConstraintViolationThrowable;

import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.spine3.gradle.protobuf.GenerationUtils.getJavaFieldName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getBuilderClassName;
import static org.spine3.gradle.protobuf.validators.ValidatingUtils.getParameterClass;

/**
 * @author Illia Shepilov
 */
//TODO:2017-03-22:illiashepilov: finish implementation.
public class MapFieldMethodConstructor extends AbstractMethodConstructor {

    private final int fieldIndex;
    private final String javaFieldName;
    private final String methodPartName;
    private final ClassName builderClassName;
    private final ClassName genericClassName;
    private final ClassName parameterClassName;
    private final FieldDescriptorProto fieldDescriptor;
    private final MapFieldType fieldType;

    private MapFieldMethodConstructor(MapFieldMethodsConstructorBuilder builder) {
        this.fieldType = (MapFieldType) builder.getFieldType();
        this.fieldIndex = builder.getFieldIndex();
        this.fieldDescriptor = builder.getFieldDescriptor();
        this.genericClassName = builder.getGenericClassName();
        this.methodPartName = getJavaFieldName(fieldDescriptor.getName(), true);
        this.javaFieldName = getJavaFieldName(fieldDescriptor.getName(), false);
        this.builderClassName = getBuilderClassName(builder.getJavaPackage(), builder.getJavaClass());
        this.parameterClassName = getParameterClass(fieldDescriptor, builder.getMessageTypeCache());
    }

    @Override
    Collection<MethodSpec> construct() {
        final List<MethodSpec> methods = newArrayList();
        methods.addAll(createMapMethods());
        methods.addAll(createRawMapMethods());
        methods.add(createCheckMapFieldMethod());
        return methods;
    }

    private List<MethodSpec> createRawMapMethods() {
        final List<MethodSpec> methods = newArrayList();
        methods.add(createPutRawMethod());
        return methods;
    }

    private List<MethodSpec> createMapMethods() {
        final List<MethodSpec> methods = newArrayList();
        methods.add(createPutMethod());

        return methods;
    }

    private MethodSpec createPutRawMethod() {
        final TypeName keyTypeName = fieldType.getKeyTypeName();
        final TypeName valueTypeName = fieldType.getValueTypeName();
        final String methodName = getJavaFieldName("put" + methodPartName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, genericClassName);
        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addException(ConversionException.class)
                                            .addParameter(String.class, "key")
                                            .addParameter(String.class, "value")
                                            .addStatement(createGetConvertedMapValue(), Map.class, keyTypeName, valueTypeName, TypeToken.class, Map.class, keyTypeName, valueTypeName)
                                            .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                            .addStatement(createValidateConvertedValueStatement(), fieldDescriptor.getName())
                                            .addStatement(javaFieldName + ".putAll(convertedValue)")
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createPutMethod() {
        final TypeName keyTypeName = fieldType.getKeyTypeName();
        final TypeName valueTypeName = fieldType.getValueTypeName();
        final String methodName = getJavaFieldName("putRaw" + methodPartName, false);
        final String descriptorCodeLine = createDescriptorCodeLine(fieldIndex, genericClassName);
        final MethodSpec result = MethodSpec.methodBuilder(methodName)
                                            .returns(builderClassName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addException(ConstraintViolationThrowable.class)
                                            .addParameter(keyTypeName, "key")
                                            .addParameter(valueTypeName, "value")
                                            .addStatement(createGetConvertedMapValue(), Map.class, keyTypeName, valueTypeName, TypeToken.class, Map.class, keyTypeName, valueTypeName)
                                            .addStatement(descriptorCodeLine, FieldDescriptor.class)
                                            .addStatement(createValidateConvertedValueStatement(), fieldDescriptor.getName())
                                            .addStatement(javaFieldName + ".putAll(convertedValue)")
                                            .addStatement(RETURN_THIS)
                                            .build();
        return result;
    }

    private MethodSpec createCheckMapFieldMethod() {
        final MethodSpec result = MethodSpec.methodBuilder("createIfNeeded")
                                            .addModifiers(Modifier.PRIVATE)
                                            .beginControlFlow("if(" + javaFieldName + " == null)")
                                            .addStatement(javaFieldName + " = new $T<>()", HashMap.class)
                                            .endControlFlow()
                                            .build();
        return result;
    }

    static MapFieldMethodsConstructorBuilder newBuilder() {
        return new MapFieldMethodsConstructorBuilder();
    }

    static class MapFieldMethodsConstructorBuilder extends MethodConstructorBuilder {
        @Override
        AbstractMethodConstructor build() {
            checkFields();
            return new MapFieldMethodConstructor(this);
        }
    }
}
