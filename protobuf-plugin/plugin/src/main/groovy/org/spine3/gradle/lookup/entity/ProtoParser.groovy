package org.spine3.gradle.lookup.entity;

/**
 * A utility class for parsing {@code .proto} files.
 *
 * @author Alexander Litus
 */
public class ProtoParser {

    private static final String COMMENT = "//";
    private static final String PACKAGE = "package";
    public static final String STATE_OF_ENTITY_OPTION = "option(state_of)=";

    private ProtoParser() {}

    /**
     * A copy of Spine EntityType enum defined in Protobuf.
     * An original enum is not used here to avoid circular dependencies between modules.
     */
    private static enum EntityType {
        NOT_SET,
        AGGREGATE,
        PROCESS_MANAGER,
        PROJECTION;
    }

    /**
     * Parses the {@code .proto} file and returns its metadata.
     *
     * @param file a {@code .proto} file to parse
     * @return a file metadata
     */
    public static ProtoFileMetadata parse(File file) {
        String protoPackage = "";
        EntityType entityType = EntityType.NOT_SET;
        final List<String> lines = file.readLines();
        for (String fullLine : lines) {
            String line = trimAndRemoveComments(fullLine)
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith(PACKAGE)) {
                protoPackage = parseProtoPackage(line);
            } else {
                if (containsEntityTypeOption(line)) {
                    entityType = parseEntityType(line);
                    if (!protoPackage.isEmpty()) {
                        break;
                    }
                }
            }
        }
        final ProtoFileMetadata metadata = new ProtoFileMetadata(protoPackage, entityType, file.getPath());
        return metadata;
    }

    private static String trimAndRemoveComments(String fullLine) {
        String result = fullLine.trim();
        if (result.contains(COMMENT)) {
            result = result.substring(0, result.indexOf(COMMENT));
        }
        return result;
    }

    private static String parseProtoPackage(String line) {
        final int packageStartIndex = line.indexOf(' ') + 1;
        final int semicolonIndex = line.indexOf(';');
        final String protoPackage = line.substring(packageStartIndex, semicolonIndex);
        return protoPackage;
    }

    private static boolean containsEntityTypeOption(String line) {
        final String lineNoSpaces = line.replace(' ', '');
        final boolean contains = lineNoSpaces.contains(STATE_OF_ENTITY_OPTION);
        return contains;
    }

    private static EntityType parseEntityType(String line) {
        for (EntityType entityType : EntityType.values()) {
            if (line.contains(entityType.toString())) {
                return entityType;
            }
        }
        return EntityType.NOT_SET;
    }

    /**
     * A {@code .proto} file metadata.
     */
    public static class ProtoFileMetadata {

        private final String protoPackage;
        private final String entityType;
        private final String fileName;

        private ProtoFileMetadata(String protoPackage, EntityType entityType, String fileName) {
            this.protoPackage = protoPackage;
            this.entityType = entityType.toString();
            this.fileName = fileName;
        }

        /**
         * Returns a Protobuf package defined in the file.
         */
        public String getProtoPackage() {
            return protoPackage;
        }

        /**
         * Returns an entity type string defined via {@code state_of} option in any message from the file.
         */
        public String getEntityType() {
            return entityType;
        }

        /**
         * Returns a name of the file.
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * Returns true if the {@code state_of} option is set in any message defined in the file, false otherwise.
         */
        public boolean isEntityStateDefined() {
            final boolean isSet = entityType != EntityType.NOT_SET.toString();
            return isSet;
        }
    }
}
