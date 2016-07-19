/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.gradle.protobuf.failures

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
    private final GString javaPackage

    private String className

    /** A map from Protobuf type name to Java class FQN. */
    private Map<GString, GString> messageTypeMap

    // https://developers.google.com/protocol-buffers/docs/proto3#scalar
    private static final Map<String, String> PROTO_FIELD_TYPES = [
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
     * Creates a new instance.
     *
     * @param failureDescriptor {@link DescriptorProto} of failure's proto message
     * @param outputFile a {@link File} to write Failure code
     * @param javaPackage Failure's java package
     * @param messageTypeMap pre-scanned map with proto types and their appropriate Java classes
     */
    FailureWriter(DescriptorProto failureDescriptor,
                  File outputFile,
                  GString javaPackage,
                  Map<GString, GString> messageTypeMap) {
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
                final Writer writer = it as OutputStreamWriter
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
        this.className = failureDescriptor.name

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
        final Set<Map.Entry<GString, GString>> fieldsEntries = readFieldValues().entrySet()
        for (int i = 0; i < fieldsEntries.size(); i++) {
            final Map.Entry<GString, GString> field = fieldsEntries.getAt(i)
            writer.write("${field.value} ${getJavaFieldName(field.key, false)}")
            final boolean isNotLast = i != (fieldsEntries.size() - 1)
            if (isNotLast) {
                writer.write(", ");
            }
        }
        writer.write(") {\n")
        writer.write("\t\tsuper(Failures.${className}.newBuilder()")
        for (Map.Entry<GString, GString> field : fieldsEntries) {
            final GString upperCaseName = getJavaFieldName(field.key, true)
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
     * @return a field name
     */
    private static GString getJavaFieldName(String protoFieldName, boolean capitalizeFirstLetter) {
        final String[] words = protoFieldName.split('_')
        GString resultName = "${words[0]}"
        for (int i = 1; i < words.length; i++) {
            final GString word = "${words[i]}"
            resultName = "${resultName}${word.charAt(0).toUpperCase()}${word.substring(1)}"
        }
        if (capitalizeFirstLetter) {
            resultName = "${resultName.charAt(0).toUpperCase()}${resultName.substring(1)}"
        }
        return resultName
    }

    /**
     * Reads all descriptor fields.
     *
     * @return name-to-value GString map
     */
    private Map<GString, GString> readFieldValues() {
        final Map<GString, GString> result = new LinkedHashMap<>()
        failureDescriptor.fieldList.each { FieldDescriptorProto field ->
            final GString value
            if (field.type == FieldDescriptorProto.Type.TYPE_MESSAGE ||
                field.type == FieldDescriptorProto.Type.TYPE_ENUM) {
                GString typeName = "$field.typeName"
                // it has a redundant dot in the beginning
                if (typeName.startsWith(".")) {
                    typeName = "${typeName.substring(1)}"
                }
                value = messageTypeMap.get(typeName)
            } else {
                value = "${PROTO_FIELD_TYPES.get(field.type.name())}"
            }
            result.put("$field.name", value)
        }
        return result
    }
}
