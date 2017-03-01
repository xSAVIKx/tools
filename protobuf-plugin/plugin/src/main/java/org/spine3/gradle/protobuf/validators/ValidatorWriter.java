package org.spine3.gradle.protobuf.validators;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.gradle.protobuf.GenerationUtils;
import org.spine3.gradle.protobuf.MessageTypeCache;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import static org.gradle.internal.impldep.com.beust.jcommander.internal.Lists.newArrayList;
import static org.spine3.gradle.protobuf.GenerationUtils.getJavaFieldName;

/**
 * Class, which writes Validator java code, based on it's descriptor.
 *
 * @author Illia Shepilov
 */
public class ValidatorWriter {

    private static final Pattern COMPILE = Pattern.compile("\\.");
    private static final String rootFolder = "generated/main/java/";
    private final MessageTypeCache messageTypeCache;
    private final WriterDto writerDto;

    public ValidatorWriter(WriterDto writerDto, MessageTypeCache messageTypeCache) {
        this.writerDto = writerDto;
        this.messageTypeCache = messageTypeCache;
    }

    public void write() {
        log().debug("Writing the java class under {}", writerDto.getJavaPackage());
        final File rootDirecroy = new File(rootFolder);

        final DescriptorProto descriptor = writerDto.getMsgDescriptor();
        final List<MethodSpec> methods = newArrayList();
        methods.addAll(createSetters(descriptor));
        try {
            final MethodSpec constructor = createConstructor();
            methods.add(constructor);
            final TypeSpec validator = createClass(methods);
            writeClass(rootDirecroy, validator);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<MethodSpec> createSetters(DescriptorProto descriptorProto) {
        final List<MethodSpec> setters = newArrayList();
        for (FieldDescriptorProto descriptorField : descriptorProto.getFieldList()) {
            Class<?> parameterClass = getParameterClass(descriptorField);
            final String paramName = getJavaFieldName(descriptorField.getName(), false);
            final ParameterSpec parameter = ParameterSpec.builder(parameterClass, paramName)
                                                         .build();
            final String methodName = "set" + getJavaFieldName(paramName, true);
            final MethodSpec methodSpec = MethodSpec.methodBuilder(methodName)
                                                    .addParameter(parameter)
                                                    .addModifiers(Modifier.PUBLIC)
                                                    .build();
            setters.add(methodSpec);
        }
        return setters;
    }

    private Class<?> getParameterClass(FieldDescriptorProto descriptorField) {
        String typeName = descriptorField.getTypeName();
        if (typeName.isEmpty()) {
            return GenerationUtils.getType(descriptorField.getType()
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

    private void writeClass(File rootFodler, TypeSpec validator) throws IOException {
        JavaFile javaFile = JavaFile.builder(writerDto.getJavaPackage(), validator)
                                    .build();
        javaFile.writeTo(rootFodler);
    }

    private MethodSpec createConstructor() {
        final MethodSpec result = MethodSpec.constructorBuilder()
                                            .addModifiers(Modifier.PRIVATE)
                                            .build();
        return result;
    }

    private TypeSpec createClass(Iterable<MethodSpec> methodSpecs) {
        final TypeSpec result = TypeSpec.classBuilder(writerDto.getJavaClass())
                                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                        .addSuperinterface(List.class) //TODO:2017-03-01:illiashepilov: replace with correct interface.
                                        .addMethods(methodSpecs)
                                        .build();
        return result;
    }

    private static String toJavaPath(String path) {
        final String result = COMPILE.matcher(path)
                                     .replaceAll(String.valueOf(File.separatorChar));
        return result;
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(ValidatorWriter.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
