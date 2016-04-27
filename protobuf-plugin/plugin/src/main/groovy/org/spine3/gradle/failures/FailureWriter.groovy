package org.spine3.gradle.failures

import com.google.protobuf.DescriptorProtos
import groovy.util.logging.Slf4j

@Slf4j
/* package */ class FailureWriter {

    private final DescriptorProtos.DescriptorProto failureDescriptor;
    private final File outputFile;
    private final String javaPackage;

    private String className;

    private Map<String, String> messageTypeMap;

    // https://developers.google.com/protocol-buffers/docs/proto3#scalar
    private static final def commonProtoTypes = [
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE.name())  : "double",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT.name())   : "float",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64.name())   : "long",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64.name())  : "long",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32.name())   : "int",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64.name()) : "long",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32.name()) : "int",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL.name())    : "boolean",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING.name())  : "String",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_GROUP.name())   : null, // GROUPS ARE NOT SUPPORTED
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES.name())   : "com.google.protobuf.ByteString",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32.name())  : "int",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32.name()): "int",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64.name()): "long",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32.name())  : "int",
            (DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64.name())  : "long",
    ]

    FailureWriter(DescriptorProtos.DescriptorProto failureDescriptor,
                  File outputFile,
                  String javaPackage,
                  Map<String, String> messageTypeMap) {
        this.failureDescriptor = failureDescriptor;
        this.outputFile = outputFile;
        this.javaPackage = javaPackage;
        this.messageTypeMap = messageTypeMap;
    }

    public void write() {
        outputFile.getParentFile().mkdirs();
        outputFile.createNewFile();

        new FileOutputStream(outputFile).withStream {
            new OutputStreamWriter(it).withWriter {
                def writer = it as OutputStreamWriter;

                writePackage(writer);
                readFieldValues();
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

    private static void writeImports(OutputStreamWriter writer) {
        writer.write("import org.spine3.server.failure.FailureThrowable;\n");
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
        final Set<Map.Entry<String, String>> fieldsEntries = readFieldValues().entrySet();
        for (int i = 0; i < fieldsEntries.size(); i++) {
            Map.Entry<String, String> field = fieldsEntries.getAt(i);

            writer.write("${field.value} ${getJavaFieldName(field.key, false)}")

            if (i != fieldsEntries.size() - 1) {
                writer.write(", ");
            }
        }
        writer.write(") {\n");
        writer.write("\t\tsuper(Failures.${className}.newBuilder()");
        for (def field : fieldsEntries) {
            def upperCaseName = getJavaFieldName(field.key, true);
            writer.write(".set${upperCaseName}(${getJavaFieldName(field.key, false)})");
        }
        writer.write(".build());\n");
        writer.write("\t}\n");
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private void writeEnding(OutputStreamWriter writer) {
        writer.write("}\n");
    }

    private static String getJavaFieldName(String protoFieldName, boolean capitalizeFirstLetter) {
        // seat_assignment_id -> SeatAssignmentId
        def words = protoFieldName.split('_');
        String resultName = words[0];
        for (int i = 1; i < words.length; i++) {
            def word = words[i]
            resultName = "${resultName}${word.charAt(0).toUpperCase()}${word.substring(1)}";
        }
        if (capitalizeFirstLetter) {
            resultName = "${resultName.charAt(0).toUpperCase()}${resultName.substring(1)}";
        }
        return resultName;
    }

    private Map<String, String> readFieldValues() {
        def fields = new LinkedHashMap<>();

        failureDescriptor.fieldList.each { DescriptorProtos.FieldDescriptorProto field ->
            def name = field.name;
            String value;

            if (field.type == DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE ||
                    field.type == DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM) {
                def fieldTypeName = field.typeName;
                // Somewhy it has a dot in the beginning
                if (fieldTypeName.startsWith(".")) {
                    fieldTypeName = "${fieldTypeName.substring(1)}";
                }
                value = messageTypeMap.get(fieldTypeName);
            } else {
                value = commonProtoTypes.get(field.type.name());
            }

            fields.put(name, value);
        }

        return fields;
    }
}
