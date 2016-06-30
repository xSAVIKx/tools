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

import com.google.common.collect.ImmutableMap
import groovy.util.logging.Slf4j

import java.util.regex.Matcher
import java.util.regex.Pattern

import static com.google.protobuf.DescriptorProtos.*

/**
 * Finds event enrichment Protobuf definitions.
 *
 * @author Alexander Litus
 */
@Slf4j
public class EnrichmentsFinder {

    /**
     * The field number of the field option `by` defined in `Spine/core-java`.
     */
    private static final String OPTION_FIELD_NUMBER_ENRICH_BY = "57125";

    /**
     * The field number of the message option `enrichment_for` defined in `Spine/core-java`.
     */
    private static final String OPTION_FIELD_NUMBER_ENRICHMENT_FOR = "57124";

    private static final Pattern OPTION_ENRICHMENT_FOR_PATTERN =
            Pattern.compile(/$OPTION_FIELD_NUMBER_ENRICHMENT_FOR: "([a-zA-Z0-9._]*)"/);

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
        final ImmutableMap.Builder<String, String> result = ImmutableMap.builder();
        final List<DescriptorProto> messages = file.getMessageTypeList();
        for (DescriptorProto msg : messages) {
            putEntry(result, msg)
        }
        return result.build();
    }

    private void putEntry(ImmutableMap.Builder<String, String> map, DescriptorProto msg) {
        final String eventName = parseEventToEnrichName(msg);
        if (eventName != null) {
            final String enrichmentName = packagePrefix + msg.getName();
            map.put(enrichmentName, eventName);
            return;
        }
        for (DescriptorProto innerMsg : msg.getNestedTypeList()) {
            for (FieldDescriptorProto field : innerMsg.getFieldList()) {
                if (hasOptionEnrichBy(field)) {
                    final String outerEventName = packagePrefix + msg.getName();
                    final String enrichmentName = outerEventName + PROTO_TYPE_SEPARATOR + innerMsg.getName();
                    map.put(enrichmentName, outerEventName);
                    return;
                }
            }
        }
    }

    private String parseEventToEnrichName(DescriptorProto msg) {
        // This needed option is "unknown" and serialized, but it is possible to print option field numbers and values.
        final String optionsStr = msg.getOptions().getUnknownFields().toString().trim();
        final Matcher matcher = OPTION_ENRICHMENT_FOR_PATTERN.matcher(optionsStr);
        if (!matcher.matches()) {
            return null;
        }
        String msgName = matcher.group(1);
        if (!msgName.contains(PROTO_TYPE_SEPARATOR)) {
            msgName = packagePrefix + msgName;
        }
        return msgName;
    }

    private static boolean hasOptionEnrichBy(FieldDescriptorProto field) {
        // This needed option is "unknown" and serialized, but it is possible to print option field numbers and values.
        final String optionsStr = field.getOptions().getUnknownFields().toString().trim();
        final boolean result = optionsStr.contains(OPTION_FIELD_NUMBER_ENRICH_BY);
        return result;
    }
}
