package org.spine3.gradle.utils;

@SuppressWarnings("UtilityClass")
class ProtoUtil {

    private static final String PACKAGE_PREFIX = "option java_package=";

    private ProtoUtil() {}

    public static String readJavaPackageFromProto(File protoFile) {
        final List<String> lines = protoFile.readLines();

        String javaPackage = null;

        for (String line : lines) {
            if (javaPackage != null) {
                break;
            }

            def trimmedLine = line.trim();
            def trimmedLineLength = trimmedLine.length();

            if (trimmedLine.startsWith(PACKAGE_PREFIX)) {
                javaPackage = trimmedLine.substring(PACKAGE_PREFIX.length(), trimmedLineLength - 1);
                javaPackage = javaPackage.replace("\"", "");
                javaPackage = javaPackage.replace(" ", "");
            } else if (javaPackage != null) {
                break;
            }
        }

        return javaPackage;
    }
}
