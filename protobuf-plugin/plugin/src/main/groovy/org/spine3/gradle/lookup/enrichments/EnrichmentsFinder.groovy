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

package org.spine3.gradle.lookup.enrichments
import groovy.util.logging.Slf4j

import static com.google.protobuf.DescriptorProtos.*

/**
 * Finds event enrichment Protobuf definitions.
 *
 * @author Alexander Litus
 */
@Slf4j
public class EnrichmentsFinder {

    /**
     * The field number of the message option `enrichment_for` defined in `Spine/core-java`.
     */
    private static final String OPTION_FIELD_NUMBER_ENRICHMENT_FOR = String.valueOf(57124);

    /**
     * The field number of the field option `by` defined in `Spine/core-java`.
     */
    private static final String OPTION_FIELD_NUMBER_ENRICH_BY = String.valueOf(57125);

    public static final String QUOTE = '"';

    public static final String PROTO_TYPE_SEPARATOR = '.';

    private final FileDescriptorProto file;
    private final String packagePrefix;

    /**
     * Creates a new instance.
     *
     * @param file a file to search enrichments in
     */
    public EnrichmentsFinder(FileDescriptorProto file) {
        this.file = file;
        this.packagePrefix = file.getPackage() + PROTO_TYPE_SEPARATOR;
    }

    /**
     * Finds event enrichment Protobuf definitions in the file.
     *
     * @return a map from enrichment type name to event to enrich type name
     */
    public Map<String, String> findEnrichments() {
        // Do not name this method "find" to avoid a confusion with "DefaultGroovyMethods.find()".
        final Map<String, String> result = new HashMap<>();
        final List<DescriptorProto> messages = file.getMessageTypeList();
        for (DescriptorProto msg : messages) {
            final String targetEventName = getEventToEnrichName(msg)
            if (targetEventName != null) {
                String enrichmentName = packagePrefix + msg.getName();
                result.put(enrichmentName, targetEventName);
            }
        }
        return result;
    }

    private String getEventToEnrichName(DescriptorProto msg) {
        final String eventToEnrichName = parseEventToEnrichName(msg);
        if (eventToEnrichName != null) {
            return eventToEnrichName;
        }
        for (FieldDescriptorProto field : msg.getFieldList()) {
            if (hasOptionEnrichBy(field)) {
                final String enrichmentName = msg.getName();
                final String outerTypeName = getOuterTypeName(enrichmentName)
                if (outerTypeName == null || containsOnlyLowercaseChars(outerTypeName)) {
                    logErrInvalidEnrichmentDeclaration(enrichmentName);
                    return null;
                }
                return outerTypeName;
            }
        }
        return null;
    }

    private static String getOuterTypeName(String enrichmentName) {
        final int lastDotIndex = enrichmentName.lastIndexOf(PROTO_TYPE_SEPARATOR);
        if (lastDotIndex < 0) {
            return null;
        }
        final String outerTypeName = enrichmentName.substring(0, lastDotIndex);
        return outerTypeName;
    }

    private static boolean containsOnlyLowercaseChars(String outerTypeName) {
        final boolean result = outerTypeName.toLowerCase().equals(outerTypeName);
        return result
    }

    private static logErrInvalidEnrichmentDeclaration(String enrichmentName) {
        log.error("Event enrichment message $enrichmentName must have 'enrichment_for' " +
                "Protobuf option, or it must be an inner type of the event to enrich.");
    }

    private String parseEventToEnrichName(DescriptorProto msg) {
        // This option is "unknown" and serialized, but it is possible to print option's field number and value.
        final String optionsStr = msg.getOptions().getUnknownFields().toString();
        if (!optionsStr.contains(OPTION_FIELD_NUMBER_ENRICHMENT_FOR)) {
            return null;
        }
        String msgName = optionsStr.substring(optionsStr.indexOf(QUOTE) + 1, optionsStr.lastIndexOf(QUOTE));
        if (!msgName.contains(packagePrefix)) {
            msgName = packagePrefix + msgName;
        }
        return msgName;
    }

    private static boolean hasOptionEnrichBy(FieldDescriptorProto field) {
        // This option is "unknown" and serialized, but it is possible to print option's field number and value.
        final String optionsStr = field.getOptions().getUnknownFields().toString();
        final boolean result = optionsStr.contains(OPTION_FIELD_NUMBER_ENRICH_BY);
        return result;
    }
}
