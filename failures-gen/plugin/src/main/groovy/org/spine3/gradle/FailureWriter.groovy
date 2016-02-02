package org.spine3.gradle

import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Descriptors
import groovy.util.logging.Slf4j
import org.gradle.api.Nullable

@Slf4j
/* package */ class FailureWriter {

    private final Descriptors.Descriptor failureDescriptor;
    private final File outputFile;
    private final String javaPackage;

    private Map<String, String> dependencyPackages;

    private String className;
    private Map<String, String> fields;

    /* package */

    FailureWriter(Descriptors.Descriptor failureDescriptor, File outputFile, String javaPackage,
                  Map<String, String> dependencyPackages) {
        this.failureDescriptor = failureDescriptor;
        this.outputFile = outputFile;
        this.javaPackage = javaPackage;
        this.dependencyPackages = dependencyPackages;
    }

    public void write() {
        outputFile.getParentFile().mkdirs();
        outputFile.createNewFile();

        new FileOutputStream(outputFile).withStream {
            new OutputStreamWriter(it).withWriter {
                def writer = it as OutputStreamWriter;

                writePackage(writer);
                writeImports(writer);
                writeClassName(writer);
                writeConstructor(writer);
                writeGetFailure(writer);
                writeEnding(writer);

                writer.flush();
            }
        }
    }

    private void writePackage(OutputStreamWriter writer) {
        writer.write("package $javaPackage;\n\n");
    }

    private void writeImports(OutputStreamWriter writer) {

        Set<String> dependencies = new HashSet<>();
        fields = new LinkedHashMap<>();

        for (Descriptors.FieldDescriptor field : failureDescriptor.getFields()) {
            switch (field.javaType) {
                case JavaType.MESSAGE:
                    def dependencyFileName = field.messageType.file.fullName;
                    def javaPackage = dependencyPackages.get(dependencyFileName);
                    def fieldType = field.messageType.name;
                    dependencies.add("$javaPackage.$fieldType");
                    fields.put(field.name, fieldType)
                    break;
                case JavaType.ENUM:
                    def dependencyFileName = field.enumType.file.fullName;
                    def javaPackage = dependencyPackages.get(dependencyFileName);
                    def fieldType = field.enumType.name;
                    dependencies.add("$javaPackage.$fieldType");
                    fields.put(field.name, fieldType);
                    break;
                case JavaType.BYTE_STRING:
                    dependencies.add("com.google.protobuf.ByteString");
                    fields.put(field.name, "ByteString");
                    break;
                default:
                    fields.put(field.name, getCommonProtoTypeName(field.getJavaType()));
            }
        }

        dependencies.add("org.spine3.server.FailureThrowable");

        for (String dependency : dependencies) {
            writer.write("import $dependency;\n");
        }

        writer.write("\n");
    }

    private void writeClassName(OutputStreamWriter writer) {
        className = failureDescriptor.name;

        writer.write("@javax.annotation.Generated(\"by Spine compiler\")\n");
        writer.write("public class $className extends FailureThrowable {\n\n");

        writer.write("\tprivate static final long serialVersionUID = 0L;\n\n");
    }

    private void writeGetFailure(OutputStreamWriter writer) {
        writer.write("\n\t@Override\n");
        writer.write("\tpublic Failures.$className getFailure() {\n");
        writer.write("\t\treturn (Failures.$className) super.getFailure();\n\t}\n");
    }

    private void writeConstructor(OutputStreamWriter writer) {
        writer.write("\tpublic $className(");
        Set<Map.Entry<String, String>> fieldsEntries = fields.entrySet();
        for (int i = 0; i < fieldsEntries.size(); i++) {
            Map.Entry<String, String> field = fieldsEntries.getAt(i);

            writer.write("${field.value} ${field.key}")

            if (i != fieldsEntries.size() - 1) {
                writer.write(", ");
            }
        }
        writer.write(") {\n");
        writer.write("\t\tsuper(Failures.${className}.newBuilder()");
        for (def field : fieldsEntries) {
            def upperCaseName = "${field.key.charAt(0).toUpperCase()}${field.key.substring(1)}";
            writer.write(".set${upperCaseName}(${field.key})");
        }
        writer.write(".build());\n");
        writer.write("\t}\n");
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private void writeEnding(OutputStreamWriter writer) {
        writer.write("}\n");
    }

    @Nullable
    private static String getCommonProtoTypeName(JavaType type) {
        switch (type) {
            case JavaType.STRING:
                return "String";
                break;

            case JavaType.INT:
            case JavaType.LONG:
            case JavaType.FLOAT:
            case JavaType.DOUBLE:
            case JavaType.BOOLEAN:
                return type.name().toLowerCase();
                break;

            default: return null;
        }
    }
}
