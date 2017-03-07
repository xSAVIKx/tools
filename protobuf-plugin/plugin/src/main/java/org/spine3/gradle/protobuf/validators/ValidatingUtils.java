package org.spine3.gradle.protobuf.validators;

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.ClassName;
import org.spine3.gradle.protobuf.GenerationUtils;
import org.spine3.gradle.protobuf.MessageTypeCache;

public class ValidatingUtils {

    private ValidatingUtils() {
        // To prevent initialization.
    }

    public static Class<?> getParameterClass(FieldDescriptorProto fieldDescriptor, MessageTypeCache messageTypeCache) {
        String typeName = fieldDescriptor.getTypeName();
        if (typeName.isEmpty()) {
            return GenerationUtils.getType(fieldDescriptor.getType()
                                                          .name());
        }
        typeName = typeName.substring(1);
        final String parameterType = messageTypeCache.getCachedTypes()
                                                     .get(typeName);
        try {
            return Class.forName(parameterType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static ClassName getBuilderClassName(String javaPackage, String javaClass) {
        final ClassName builderClassName = ClassName.get(javaPackage, javaClass);
        return builderClassName;
    }
}
