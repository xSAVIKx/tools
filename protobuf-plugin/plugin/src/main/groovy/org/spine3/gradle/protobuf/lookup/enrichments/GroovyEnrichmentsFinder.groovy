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

package org.spine3.gradle.protobuf.lookup.enrichments

import com.google.common.base.Joiner
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import groovy.util.logging.Slf4j

import java.util.regex.Pattern

import static com.google.common.collect.Lists.newLinkedList
import static com.google.protobuf.DescriptorProtos.*
import static java.util.AbstractMap.SimpleEntry
import static org.spine3.gradle.protobuf.util.UnknownOptions.getUnknownOptionValue
import static org.spine3.gradle.protobuf.util.UnknownOptions.hasUnknownOption

/**
 * Finds event enrichment Protobuf definitions.
 *
 * @author Alexander Litus
 */
@Slf4j
@SuppressWarnings("UnnecessaryQualifiedReference")
class GroovyEnrichmentsFinder {

    /**
     * The field number of the field option `by` defined in `Spine/core-java`.
     */
    private static final Long OPTION_NUMBER_ENRICH_BY = 57125L

    /**
     * The field number of the message option `enrichment_for` defined in `Spine/core-java`.
     */
    private static final Long OPTION_NUMBER_ENRICHMENT_FOR = 57124L

    /**
     * The field number of the message option `enrichment` defined in `Spine/core-java`.
     */
    private static final Long OPTION_NUMBER_ENRICHMENT = 57126L


    private static final Pattern PATTERN_SPACE = Pattern.compile(" ")

    private static final String TARGET_NAME_SEPARATOR = ","
    private static final Pattern PATTERN_TARGET_NAME_SEPARATOR = Pattern.compile(TARGET_NAME_SEPARATOR)

    private static final String PROTO_TYPE_SEPARATOR = "."

    private final FileDescriptorProto file
    private final GString packagePrefix

    /**
     * Creates a new instance.
     *
     * @param file a file to search enrichments in
     */
    GroovyEnrichmentsFinder(FileDescriptorProto file) {
        this.file = file
        this.packagePrefix = "$file.package$PROTO_TYPE_SEPARATOR"
    }

    /**
     * Finds event enrichment Protobuf definitions in the file.
     *
     * @return a map from enrichment type name to event to enrich type name
     */
    Map<GString, GString> findEnrichments() {
        // Do not name this method "find" to avoid a confusion with "DefaultGroovyMethods.find()".
        final ImmutableMap.Builder<GString, GString> result = ImmutableMap.builder()
        final List<DescriptorProto> messages = file.getMessageTypeList()
        for (DescriptorProto msg : messages) {
            putEntry(result, msg)
        }
        return result.build()
    }

    private void putEntry(ImmutableMap.Builder<GString, GString> mapBuilder, DescriptorProto msg) {
        final Map<GString, GString> entries = scanMsg(msg)
        for (Map.Entry<GString, GString> entry : entries.entrySet()) {
            put(entry, mapBuilder)
        }
        if(!entries.isEmpty()) {
            return
        }
        final Map.Entry<GString, GString> entryFromField = scanFields(msg)
        if (entryFromField) {
            put(entryFromField, mapBuilder)
            return
        }
        final Map.Entry<GString, GString> entryFromInnerMsg = scanInnerMessages(msg)
        if (entryFromInnerMsg) {
            put(entryFromInnerMsg, mapBuilder)
        }
    }

    private Map<GString, GString> scanMsg(DescriptorProto msg) {
        final ImmutableMap.Builder<GString, GString> msgScanResultBuilder = ImmutableMap.builder()
        final GString messageName = "$packagePrefix$msg.name"

        // Treating current {@code msg} as an enrichment object.
        final GString eventNames = parseEventNamesFromMsgOption(msg)
        if (eventNames) {
            msgScanResultBuilder.put(messageName, eventNames)
        }

        // Treating current {@code msg} as a target for enrichment (e.g. Spine event).
        final List<String> enrichmentNames = parseEnrichmentNamesFromMsgOption(msg)
        for (String enrichmentName : enrichmentNames) {
            msgScanResultBuilder.put("${enrichmentName}", messageName)
        }

        return msgScanResultBuilder.build();
    }

    private Map.Entry<GString, GString> scanFields(DescriptorProto msg) {
        final String msgName = msg.getName()
        for (FieldDescriptorProto field : msg.getFieldList()) {
            if (hasOptionEnrichBy(field)) {
                final GString eventNameFromBy = parseEventNameFromOptBy(field)
                if (!eventNameFromBy) {
                    throw invalidByOptionValue(msgName)
                }
                final GString enrichmentName = "$packagePrefix$msgName"
                return new SimpleEntry<>(enrichmentName, eventNameFromBy)
            }
        }
        return null
    }

    private Map.Entry<GString, GString> scanInnerMessages(DescriptorProto msg) {
        for (DescriptorProto innerMsg : msg.getNestedTypeList()) {
            for (FieldDescriptorProto field : innerMsg.getFieldList()) {
                if (hasOptionEnrichBy(field)) {
                    final GString outerEventName = "$packagePrefix$msg.name"
                    final GString enrichmentName = "$outerEventName$PROTO_TYPE_SEPARATOR$innerMsg.name"
                    return new SimpleEntry<>(enrichmentName, outerEventName)
                }
            }
        }
        return null
    }

    private GString parseEventNamesFromMsgOption(DescriptorProto msg) {
        final String eventNamesStr = getUnknownOptionValue(msg, OPTION_NUMBER_ENRICHMENT_FOR)
        if (!eventNamesStr) {
            return null
        }
        final Collection<String> eventNamesPrimary = parseTargetNames(eventNamesStr)
        final Collection<String> eventNamesResult = newLinkedList()
        for (String eventName : eventNamesPrimary) {
            final boolean isFqn = eventName.contains(PROTO_TYPE_SEPARATOR)
            if (isFqn) {
                eventNamesResult.add(eventName)
            } else {
                eventNamesResult.add(packagePrefix + eventName)
            }
        }
        final String result = Joiner.on(TARGET_NAME_SEPARATOR).join(eventNamesResult)
        return "$result"
    }

    private List<String> parseEnrichmentNamesFromMsgOption(DescriptorProto msg) {
        final String enrichmentNames = getUnknownOptionValue(msg, OPTION_NUMBER_ENRICHMENT)
        if (!enrichmentNames) {
            return null
        }
        final Collection<String> enrichmentNamesPrimary = parseTargetNames(enrichmentNames)
        final Collection<String> enrichmentNamesResult = newLinkedList()
        for (String enrichmentName : enrichmentNamesPrimary) {
            final boolean isFqn = enrichmentName.contains(PROTO_TYPE_SEPARATOR)
            if (isFqn) {
                enrichmentNamesResult.add(enrichmentName)
            } else {
                enrichmentNamesResult.add(packagePrefix + enrichmentName)
            }
        }
        return enrichmentNamesResult
    }

    private static List<String> parseTargetNames(String targetNames) {
        String names = targetNames
        names = PATTERN_SPACE.matcher(names).replaceAll("")
        final String[] namesArray = PATTERN_TARGET_NAME_SEPARATOR.split(names)
        return ImmutableList.copyOf(namesArray)
    }

    private static boolean hasOptionEnrichBy(FieldDescriptorProto field) {
        final boolean hasOption = hasUnknownOption(field, OPTION_NUMBER_ENRICH_BY)
        return hasOption
    }

    private static GString parseEventNameFromOptBy(FieldDescriptorProto field) {
        final String fieldFqn = getUnknownOptionValue(field, OPTION_NUMBER_ENRICH_BY)
        if (!fieldFqn) {
            return null
        }
        final int index = fieldFqn.lastIndexOf(PROTO_TYPE_SEPARATOR)
        if (index < 0) {
            return null
        }
        final String eventName = fieldFqn.substring(0, index)
        return "$eventName"
    }

    private static void put(Map.Entry<String, String> entry, ImmutableMap.Builder<String, String> mapBuilder) {
        // put key and value separately to avoid an error
        mapBuilder.put(entry.getKey(), entry.getValue())
    }

    private static RuntimeException invalidByOptionValue(String msgName) {
        throw new RuntimeException("Field of message '${msgName}' has invalid 'by' option value, " +
                "which must be the fully-qualified field reference.")
    }
}
