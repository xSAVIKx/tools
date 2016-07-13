package org.spine3.gradle.failures

import groovy.util.logging.Slf4j

import static com.google.protobuf.DescriptorProtos.DescriptorProto
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto

/**
 * Class, which writes Failure java code, based on it's descriptor.
 */
@Slf4j
class FailureWriter {

    private final DescriptorProto failureDescriptor
    private final File outputFile
    private final String javaPackage

    private String className

    private Map<String, String> messageTypeMap

    // https://developers.google.com/protocol-buffers/docs/proto3#scalar
    private static final def commonProtoTypes = [
            (FieldDescriptorProto.Type.TYPE_DOUBLE.name())  : "double",
            (FieldDescriptorProto.Type.TYPE_FLOAT.name())   : "float",
            (FieldDescriptorProto.Type.TYPE_INT64.name())   : "long",
            (FieldDescriptorProto.Type.TYPE_UINT64.name())  : "long",
            (FieldDescriptorProto.Type.TYPE_INT32.name())   : "int",
            (FieldDescriptorProto.Type.TYPE_FIXED64.name()) : "long",
            (FieldDescriptorProto.Type.TYPE_FIXED32.name()) : "int",
            (FieldDescriptorProto.Type.TYPE_BOOL.name())    : "boolean",
            (FieldDescriptorProto.Type.TYPE_STRING.name())  : "String",
            (FieldDescriptorProto.Type.TYPE_GROUP.name())   : null, // GROUPS ARE NOT SUPPORTED
            (FieldDescriptorProto.Type.TYPE_BYTES.name())   : "com.google.protobuf.ByteString",
            (FieldDescriptorProto.Type.TYPE_UINT32.name())  : "int",
            (FieldDescriptorProto.Type.TYPE_SFIXED32.name()): "int",
            (FieldDescriptorProto.Type.TYPE_SFIXED64.name()): "long",
            (FieldDescriptorProto.Type.TYPE_SINT32.name())  : "int",
            (FieldDescriptorProto.Type.TYPE_SINT64.name())  : "long",
    ]

    /**
     * Public constructor for {@code FailureWriter}.
     *
     * @param failureDescriptor {@link DescriptorProto} of failure's proto message
     * @param outputFile a {@link File} to write Failure code
     * @param javaPackage Failure's java package
     * @param messageTypeMap pre-scanned map with proto types and their appropriate Java classes
     */
    FailureWriter(DescriptorProto failureDescriptor,
                  File outputFile,
                  String javaPackage,
                  Map<String, String> messageTypeMap) {
        this.failureDescriptor = failureDescriptor
        this.outputFile = outputFile
        this.javaPackage = javaPackage
        this.messageTypeMap = messageTypeMap
    }

    /**
     * Initiates writing.
     */
    void write() {
        outputFile.getParentFile().mkdirs()
        outputFile.createNewFile()

        new FileOutputStream(outputFile).withStream {
            new OutputStreamWriter(it).withWriter {
                def writer = it as OutputStreamWriter

                writePackage(writer)
                readFieldValues()
                writeImports(writer)
                writeClassName(writer)
                writeConstructor(writer)
                writeGetFailure(writer)
                writeEnding(writer)

                writer.flush()
            }
        }
    }

    private void writePackage(OutputStreamWriter writer) {
        writer.write("package $javaPackage;\n\n")
    }

    private static void writeImports(OutputStreamWriter writer) {
        writer.write("import org.spine3.base.FailureThrowable;\n")
        writer.write("\n")
    }

    private void writeClassName(OutputStreamWriter writer) {
        className = failureDescriptor.name

        writer.write("@javax.annotation.Generated(\"by Spine compiler\")\n")
        writer.write("public class $className extends FailureThrowable {\n\n")

        writer.write("\tprivate static final long serialVersionUID = 0L;\n\n")
    }

    private void writeGetFailure(OutputStreamWriter writer) {
        writer.write("\n\t@Override\n")
        writer.write("\tpublic Failures.$className getFailure() {\n")
        writer.write("\t\treturn (Failures.$className) super.getFailure();\n\t}\n")
    }

    private void writeConstructor(OutputStreamWriter writer) {
        writer.write("\tpublic $className(")
        final Set<Map.Entry<String, String>> fieldsEntries = readFieldValues().entrySet()
        for (int i = 0; i < fieldsEntries.size(); i++) {
            Map.Entry<String, String> field = fieldsEntries.getAt(i)

            writer.write("${field.value} ${getJavaFieldName(field.key, false)}")

            if (i != fieldsEntries.size() - 1) {
                writer.write(", ");
            }
        }
        writer.write(") {\n")
        writer.write("\t\tsuper(Failures.${className}.newBuilder()")
        for (def field : fieldsEntries) {
            def upperCaseName = getJavaFieldName(field.key, true)
            writer.write(".set${upperCaseName}(${getJavaFieldName(field.key, false)})")
        }
        writer.write(".build());\n")
        writer.write("\t}\n")
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private void writeEnding(OutputStreamWriter writer) {
        writer.write("}\n")
    }

    /**
     * Transforms Protobuf-style field name into corresponding Java-style field name.
     *
     * <p>For example, seat_assignment_id -> SeatAssignmentId
     *
     * @param protoFieldName Protobuf field name.
     * @param capitalizeFirstLetter Indicates if we need first letter of the output to be capitalized.
     * @return field name String.
     */
    private static String getJavaFieldName(String protoFieldName, boolean capitalizeFirstLetter) {
        def words = protoFieldName.split('_')
        String resultName = words[0]
        for (int i = 1; i < words.length; i++) {
            def word = words[i]
            resultName = "${resultName}${word.charAt(0).toUpperCase()}${word.substring(1)}"
        }
        if (capitalizeFirstLetter) {
            resultName = "${resultName.charAt(0).toUpperCase()}${resultName.substring(1)}"
        }
        return resultName
    }

    /**
     * Reads all descriptor's fields.
     *
     * @return name-to-value String map.
     */
    private Map<String, String> readFieldValues() {
        def fields = new LinkedHashMap<>()
        failureDescriptor.fieldList.each { FieldDescriptorProto field ->
            def name = field.name
            String value
            if (field.type == FieldDescriptorProto.Type.TYPE_MESSAGE ||
                    field.type == FieldDescriptorProto.Type.TYPE_ENUM) {
                def fieldTypeName = field.typeName
                // Somewhy it has a dot in the beginning
                if (fieldTypeName.startsWith(".")) {
                    fieldTypeName = "${fieldTypeName.substring(1)}"
                }
                value = messageTypeMap.get(fieldTypeName)
            } else {
                value = commonProtoTypes.get(field.type.name())
            }
            fields.put(name, value)
        }
        return fields
    }
}
