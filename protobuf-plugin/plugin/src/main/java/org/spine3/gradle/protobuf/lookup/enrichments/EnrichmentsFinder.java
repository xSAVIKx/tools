/*
 *
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
 *
 */
package org.spine3.gradle.protobuf.lookup.enrichments;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.protobuf.DescriptorProtos;

import java.util.*;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newLinkedList;
import static org.spine3.gradle.protobuf.util.UnknownOptions.getUnknownOptionValue;
import static org.spine3.gradle.protobuf.util.UnknownOptions.hasUnknownOption;

/**
 * Finds event enrichment Protobuf definitions.
 *
 * @author Alexander Litus, Alex Tymchenko
 */
/* package */ class EnrichmentsFinder {

    /**
     * The field number of the field option `by` defined in `Spine/core-java`.
     */
    private static final Long OPTION_NUMBER_ENRICH_BY = 57125L;

    /**
     * The field number of the message option `enrichment_for` defined in `Spine/core-java`.
     */
    private static final Long OPTION_NUMBER_ENRICHMENT_FOR = 57124L;

    /**
     * The field number of the message option `enrichment` defined in `Spine/core-java`.
     */
    private static final Long OPTION_NUMBER_ENRICHMENT = 57126L;

    private static final String TARGET_NAME_SEPARATOR = ",";
    private static final String PROTO_TYPE_SEPARATOR = ".";

    /**
     * Wildcard option used in By field option.
     *
     * <p>{@code string enrichment_value [(by) = "*.my_event_id"];} tells that this enrichment may have any target
     * event types. That's why an FQN of the target type is replaced by this wildcard option.
     **/
    private static final String ANY_BY_OPTION_TARGET = "*";
    private static final Pattern PATTERN_SPACE = Pattern.compile(" ");
    private static final Pattern PATTERN_TARGET_NAME_SEPARATOR = Pattern.compile(TARGET_NAME_SEPARATOR);

    private final DescriptorProtos.FileDescriptorProto file;
    private final String packagePrefix;

    /**
     * Creates a new instance.
     *
     * @param file a file to search enrichments in
     */
    EnrichmentsFinder(DescriptorProtos.FileDescriptorProto file) {
        this.file = file;
        this.packagePrefix = file.getPackage() + PROTO_TYPE_SEPARATOR;
    }

    /**
     * Finds event enrichment Protobuf definitions in the file.
     *
     * @return a map from enrichment type name to event to enrich type name
     */
    Map<String, String> findEnrichments() {
        final HashMultimap<String, String> result = HashMultimap.create();
        final List<DescriptorProtos.DescriptorProto> messages = file.getMessageTypeList();
        for (DescriptorProtos.DescriptorProto msg : messages) {
            putEntry(result, msg);
        }

        /**
         * Merge duplicate key values, if any.
         *
         * <p>That may happen when the wildcard `By` option values are handled,
         * i.e. same enrichment as a key, but multiple target event types.
         **/

        final ImmutableMap.Builder<String, String> mergedResult = ImmutableMap.builder();
        for (String key : result.keySet()) {
            final Set<String> valuesPerKey = result.get(key);

            final String mergedValue;
            if (valuesPerKey.size() > 1) {
                mergedValue = Joiner.on(TARGET_NAME_SEPARATOR).join(valuesPerKey);
            } else {
                mergedValue = valuesPerKey.iterator().next();
            }
            mergedResult.put(key, mergedValue);
        }

        return mergedResult.build();
    }

    private void putEntry(Multimap<String, String> targetMap, DescriptorProtos.DescriptorProto msg) {
        final Map<String, String> entries = scanMsg(msg);
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            put(entry, targetMap);
        }
        if (!entries.isEmpty()) {
            return;
        }
        final Map.Entry<String, String> entryFromField = scanFields(msg);
        if (entryFromField != null) {
            put(entryFromField, targetMap);
            return;
        }
        final Map.Entry<String, String> entryFromInnerMsg = scanInnerMessages(msg);
        if (entryFromInnerMsg != null) {
            put(entryFromInnerMsg, targetMap);
        }
    }

    private Map<String, String> scanMsg(DescriptorProtos.DescriptorProto msg) {
        final ImmutableMap.Builder<String, String> msgScanResultBuilder = ImmutableMap.builder();
        final String messageName = packagePrefix + msg.getName();

        // Treating current {@code msg} as an enrichment object.
        final String eventNames = parseEventNamesFromMsgOption(msg);
        if (eventNames != null) {
            msgScanResultBuilder.put(messageName, eventNames);
        }

        // Treating current {@code msg} as a target for enrichment (e.g. Spine event).
        final Collection<String> enrichmentNames = parseEnrichmentNamesFromMsgOption(msg);
        if (enrichmentNames != null) {
            for (String enrichmentName : enrichmentNames) {
                msgScanResultBuilder.put(enrichmentName, messageName);
            }
        }

        return msgScanResultBuilder.build();
    }

    private Map.Entry<String, String> scanFields(DescriptorProtos.DescriptorProto msg) {
        final String msgName = msg.getName();
        for (DescriptorProtos.FieldDescriptorProto field : msg.getFieldList()) {
            if (hasOptionEnrichBy(field)) {
                final String eventNameFromBy = parseEventNameFromOptBy(field);
                if (ANY_BY_OPTION_TARGET.equals(eventNameFromBy)) {

                    // Ignore the wildcard By options, as we don't know the target event type in this case.
                    continue;
                }
                if (eventNameFromBy == null) {
                    throw invalidByOptionValue(msgName);
                }
                final String enrichmentName = packagePrefix + msgName;
                return new AbstractMap.SimpleEntry<>(enrichmentName, eventNameFromBy);
            }
        }
        return null;
    }

    private Map.Entry<String, String> scanInnerMessages(DescriptorProtos.DescriptorProto msg) {
        for (DescriptorProtos.DescriptorProto innerMsg : msg.getNestedTypeList()) {
            for (DescriptorProtos.FieldDescriptorProto field : innerMsg.getFieldList()) {
                if (hasOptionEnrichBy(field)) {
                    final String outerEventName = packagePrefix + msg.getName();
                    final String enrichmentName = outerEventName + PROTO_TYPE_SEPARATOR + innerMsg.getName();
                    return new AbstractMap.SimpleEntry<>(enrichmentName, outerEventName);
                }
            }
        }
        return null;
    }

    private String parseEventNamesFromMsgOption(DescriptorProtos.DescriptorProto msg) {
        final String eventNamesStr = getUnknownOptionValue(msg, OPTION_NUMBER_ENRICHMENT_FOR);
        if (eventNamesStr == null) {
            return null;
        }
        final Collection<String> eventNamesResult = normalizeTypeNames(eventNamesStr);
        return Joiner.on(TARGET_NAME_SEPARATOR).join(eventNamesResult);
    }

    private Collection<String> parseEnrichmentNamesFromMsgOption(DescriptorProtos.DescriptorProto msg) {
        final String enrichmentNames = getUnknownOptionValue(msg, OPTION_NUMBER_ENRICHMENT);
        if (enrichmentNames == null) {
            return null;
        }
        return normalizeTypeNames(enrichmentNames);
    }

    /**
     * Parse type names from a String and supply them with the package prefix if it is not present.
     *
     * @param typeNamesAsString the type names as a single String
     * @return a collection of normalized type names
     */
    private Collection<String> normalizeTypeNames(String typeNamesAsString) {
        final Collection<String> targetNamesPrimary = parseTargetNames(typeNamesAsString);
        final Collection<String> normalizedNames = newLinkedList();
        for (String typeName : targetNamesPrimary) {
            final boolean isFqn = typeName.contains(PROTO_TYPE_SEPARATOR);
            if (isFqn) {
                normalizedNames.add(typeName);
            } else {
                normalizedNames.add(packagePrefix + typeName);
            }
        }
        return normalizedNames;
    }

    private static List<String> parseTargetNames(String targetNames) {
        final String names = PATTERN_SPACE.matcher(targetNames).replaceAll("");
        final String[] namesArray = PATTERN_TARGET_NAME_SEPARATOR.split(names);
        return ImmutableList.copyOf(namesArray);
    }

    private static boolean hasOptionEnrichBy(DescriptorProtos.FieldDescriptorProto field) {
        return hasUnknownOption(field, OPTION_NUMBER_ENRICH_BY);
    }

    private static String parseEventNameFromOptBy(DescriptorProtos.FieldDescriptorProto field) {
        final String fieldFqn = getUnknownOptionValue(field, OPTION_NUMBER_ENRICH_BY);
        if (fieldFqn == null) {
            return null;
        }
        final int index = fieldFqn.lastIndexOf(PROTO_TYPE_SEPARATOR);
        if (index < 0) {
            return null;
        }
        final String result = fieldFqn.substring(0, index);
        return result;
    }

    private static void put(Map.Entry<String, String> entry, Multimap<String, String> targetMap) {
        // put key and value separately to avoid an error
        targetMap.put(entry.getKey(), entry.getValue());
    }

    private static RuntimeException invalidByOptionValue(String msgName) {
        throw new RuntimeException("Field of message `" + msgName + "` has invalid 'by' option value, " +
                "which must be the fully-qualified field reference.");
    }

}
