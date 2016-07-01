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
import static java.util.AbstractMap.*

/**
 * Finds event enrichment Protobuf definitions.
 *
 * @author Alexander Litus
 */
@Slf4j
@SuppressWarnings("UnnecessaryQualifiedReference")
public class EnrichmentsFinder {

    private static final String MSG_FQN_REGEX = /[a-zA-Z0-9._]+/;

    /**
     * The field number of the field option `by` defined in `Spine/core-java`.
     */
    private static final String OPTION_FIELD_NUMBER_ENRICH_BY = "57125";

    private static final Pattern OPTION_PATTERN_ENRICH_BY =
            Pattern.compile(/$OPTION_FIELD_NUMBER_ENRICH_BY: "($MSG_FQN_REGEX)"/);

    /**
     * The field number of the message option `enrichment_for` defined in `Spine/core-java`.
     */
    private static final String OPTION_FIELD_NUMBER_ENRICHMENT_FOR = "57124";

    private static final Pattern OPTION_PATTERN_ENRICHMENT_FOR =
            Pattern.compile(/$OPTION_FIELD_NUMBER_ENRICHMENT_FOR: "($MSG_FQN_REGEX)"/);

    private static final String PROTO_TYPE_SEPARATOR = '.';

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

    private void putEntry(ImmutableMap.Builder<String, String> mapBuilder, DescriptorProto msg) {
        final Map.Entry entry = scanMsg(msg);
        if (entry != null) {
            put(entry, mapBuilder);
            return;
        }
        final Map.Entry entryFromField = scanFields(msg);
        if (entryFromField != null) {
            put(entryFromField, mapBuilder);
            return;
        }
        final Map.Entry entryFromInnerMsg = scanInnerMessages(msg);
        if (entryFromInnerMsg != null) {
            put(entryFromInnerMsg, mapBuilder);
        }
    }

    private Map.Entry<String, String> scanMsg(DescriptorProto msg) {
        final String eventName = parseEventNameFromMsgOption(msg);
        if (eventName == null) {
            return null;
        }
        final String enrichmentName = packagePrefix + msg.getName();
        return new SimpleEntry<>(enrichmentName, eventName);
    }

    private Map.Entry<String, String> scanFields(DescriptorProto msg) {
        final String msgName = msg.getName();
        for (FieldDescriptorProto field : msg.getFieldList()) {
            if (hasOptionEnrichBy(field)) {
                final String eventNameFromBy = parseEventNameFromOptBy(field);
                if (eventNameFromBy == null) {
                    throw invalidByOptionValue(msgName);
                }
                final String enrichmentName = packagePrefix + msgName;
                return new SimpleEntry<>(enrichmentName, eventNameFromBy);
            }
        }
        return null;
    }

    private Map.Entry<String, String> scanInnerMessages(DescriptorProto msg) {
        for (DescriptorProto innerMsg : msg.getNestedTypeList()) {
            for (FieldDescriptorProto field : innerMsg.getFieldList()) {
                if (hasOptionEnrichBy(field)) {
                    final String outerEventName = packagePrefix + msg.getName();
                    final String enrichmentName = outerEventName + PROTO_TYPE_SEPARATOR + innerMsg.getName();
                    return new SimpleEntry<>(enrichmentName, outerEventName);
                }
            }
        }
        return null;
    }

    private String parseEventNameFromMsgOption(DescriptorProto msg) {
        // This needed option is "unknown" and serialized, but it is possible to print option field numbers and values.
        final String optionsStr = msg.getOptions().getUnknownFields().toString().trim();
        final Matcher matcher = OPTION_PATTERN_ENRICHMENT_FOR.matcher(optionsStr);
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
        final String optionsStr = getOptionsString(field)
        final boolean result = optionsStr.contains(OPTION_FIELD_NUMBER_ENRICH_BY);
        return result;
    }

    private static String parseEventNameFromOptBy(FieldDescriptorProto field) {
        final String optionsStr = getOptionsString(field);
        final Matcher matcher = OPTION_PATTERN_ENRICH_BY.matcher(optionsStr);
        if (!matcher.matches()) {
            return null;
        }
        final String fieldFqn = matcher.group(1);
        final String eventName = fieldFqn.substring(0, fieldFqn.lastIndexOf(PROTO_TYPE_SEPARATOR));
        return eventName;
    }

    private static String getOptionsString(FieldDescriptorProto field) {
        // This needed option is "unknown" and serialized, but it is possible to print option field numbers and values.
        final String optionsStr = field.getOptions().getUnknownFields().toString().trim();
        return optionsStr;
    }

    private static void put(Map.Entry<String, String> entry, ImmutableMap.Builder<String, String> mapBuilder) {
        mapBuilder.put(entry.getKey(), entry.getValue());
    }

    private static RuntimeException invalidByOptionValue(String msgName) {
        throw new RuntimeException("Field of message '${msgName}' has invalid 'by' option value, " +
                "which must be the fully-qualified field reference.")
    }
}
