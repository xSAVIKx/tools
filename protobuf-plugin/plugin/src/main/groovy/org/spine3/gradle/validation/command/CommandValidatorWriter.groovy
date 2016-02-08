package org.spine3.gradle.validation.command;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.GeneratedMessage
import groovy.util.logging.Slf4j;

@Slf4j
class CommandValidatorWriter {

    private final File outputFile;
    private final Descriptors.FileDescriptor commandsDescriptor;
    private final String javaPackage;
    private final String className;

    private final ClassLoader classLoader;

    public CommandValidatorWriter(File outputFile, Descriptors.FileDescriptor commandsDescriptor, String javaPackage,
                                  String className, ClassLoader classLoader) {
        this.outputFile = outputFile;
        this.commandsDescriptor = commandsDescriptor;
        this.javaPackage = javaPackage;
        this.className = className;

        this.classLoader = classLoader;
    }

    public void writeValidator() {
        new FileOutputStream(outputFile).withStream {
            new OutputStreamWriter(it).withWriter {
                def writer = it as OutputStreamWriter;

                writePackage(writer);
                writeImports(writer);
                writeClassName(writer);
                writeConstructor(writer);

                writeContents(writer);

                writeEnding(writer);

                writer.flush();
            }
        }
    }

    private void writePackage(OutputStreamWriter writer) {
        writer.write("package $javaPackage;\n\n");
    }

    private static void writeImports(OutputStreamWriter writer) {
        writer.write("import static com.google.common.base.Preconditions.checkState;\n");
        writer.write("import static java.lang.String.format;\n");
//        writer.write("import static org.spine3.samples.lobby.registration.util.ValidationUtils.*;\n");
        writer.write("\n");
    }

    private void writeClassName(OutputStreamWriter writer) {
        writer.write("@javax.annotation.Generated(\"by Spine compiler\")\n");
        writer.write("@SuppressWarnings({\"TypeMayBeWeakened\"/** \"OrBuilder\" parameters are not applicable*/, \"UtilityClass\"})\n");
        writer.write("public class $className {\n\n");
    }

    private void writeConstructor(OutputStreamWriter writer) {
        writer.write("\tprivate $className() {}\n\n");
    }

    private static void writeEnding(OutputStreamWriter writer) {
        writer.write("}\n");
    }

    private void writeContents(OutputStreamWriter writer) {
        for (Descriptors.Descriptor commandDescriptor : commandsDescriptor.messageTypes) {
            writer.write("\tstatic void validateCommand(${commandDescriptor.name} cmd) {\n");
            for (FieldDescriptor fieldDescriptor : commandDescriptor.fields) {
                writeFieldValidation(writer, fieldDescriptor);
            }
            writer.write("\t}\n\n")
        }
    }

    private void writeFieldValidation(OutputStreamWriter writer, FieldDescriptor field) {

        Class clazz = classLoader.loadClass("org.spine3.sample.commands.order.OrderCommandsProto");

        if (clazz == null) {
            log.error("Could not load options.");
            return;
        }

        GeneratedMessage.GeneratedExtension<DescriptorProtos.FieldOptions, Boolean> requiredOption =
                clazz.getField("required").get(null) as GeneratedMessage.GeneratedExtension<DescriptorProtos.FieldOptions, Boolean>;

        // TODO:2016-02-08: Move extension definition somewhere.
        // org.spine3.sample.commands.order.OrderCommandsProto.required

        def extension = field.options.getExtension(requiredOption);
        if (extension != null && extension) {
            def fieldName = getFieldName(field);
            writer.write("\t\tif (!cmd.has${fieldName}()) {\n");
            writer.write("\t\t\tfinal String message = format(\"The field ${fieldName} must be defined in all messages of class: ${className}.\");\n");
            writer.write("\t\t\tthrow new IllegalArgumentException(message);\n");
            writer.write("\t\t}\n");
        }
    }

    private static String getFieldName(FieldDescriptor fieldDescriptor) {
        final String originalName = fieldDescriptor.name;
        final String[] words = originalName.split("_");
        String newName = "";
        for (String word : words) {
            if (word.size() > 0) {
                newName = "$newName${word.charAt(0).toUpperCase()}${word.substring(1)}";
            }
        }
        return newName;
    }
}
