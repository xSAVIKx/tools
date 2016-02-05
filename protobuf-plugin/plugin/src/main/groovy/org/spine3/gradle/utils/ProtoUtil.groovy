package org.spine3.gradle.utils

import com.google.protobuf.Descriptors
import com.google.protobuf.Message
import groovy.util.logging.Slf4j

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method;

@Slf4j
@SuppressWarnings("UtilityClass")
class ProtoUtil {

    private static final String METHOD_GET_DESCRIPTOR = "getDescriptor";
    private static final String PACKAGE_PREFIX = "option java_package=";
    private static final String MESSAGE_PREFIX = "message ";

    private ProtoUtil() {}

    public static String readJavaPackageFromProto(File protoFile) {
        return readProtoMetadata(protoFile).javaPackage;
    }

    public static ProtoFileMetadata readProtoMetadata(File protoFile) {
        final List<String> lines = protoFile.readLines();

        String javaPackage = null;
        String firstFoundMessageName = null;

        for (String line : lines) {
            if (javaPackage != null && firstFoundMessageName != null) {
                break;
            }

            def trimmedLine = line.trim();
            def trimmedLineLength = trimmedLine.length();

            if (trimmedLine.startsWith(PACKAGE_PREFIX)) {
                javaPackage = trimmedLine.substring(PACKAGE_PREFIX.length(), trimmedLineLength - 1);
                javaPackage = javaPackage.replace("\"", "");
                javaPackage = javaPackage.replace(" ", "");
            } else if (trimmedLine.startsWith(MESSAGE_PREFIX)) {
                firstFoundMessageName = trimmedLine.substring(MESSAGE_PREFIX.length());
                firstFoundMessageName = firstFoundMessageName.replace("{", "");
                firstFoundMessageName = firstFoundMessageName.replace(" ", "");
            }
        }

        return new ProtoFileMetadata(javaPackage, firstFoundMessageName);
    }

    /**
     * Metadata contains fields, which are useful for acquiring descriptor.
     */
    public static class ProtoFileMetadata {
        public final String javaPackage;
        public final String firstFoundMessageName;

        public ProtoFileMetadata(String javaPackage, String firstFoundMessageName) {
            this.javaPackage = javaPackage;
            this.firstFoundMessageName = firstFoundMessageName;
        }
    }

    public static Descriptors.GenericDescriptor getClassDescriptor(Class clazz) {
        if (!Message.class.isAssignableFrom(clazz)) {
            log.error("Class " + clazz + " is not an instance of Protobuf Message");
            return null;
        }
        try {
            final Method method = clazz.getMethod(METHOD_GET_DESCRIPTOR);
            final Descriptors.GenericDescriptor result = (Descriptors.GenericDescriptor) method.invoke(null);
            return result;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            log.error("Could not get descriptor for type " + clazz.getName());
            return null;
        }
    }
}
