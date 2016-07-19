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

import java.util.regex.Matcher
import java.util.regex.Pattern

import static com.google.common.collect.Lists.newLinkedList
import static com.google.protobuf.DescriptorProtos.*
import static java.util.AbstractMap.SimpleEntry

/**
 * Finds event enrichment Protobuf definitions.
 *
 * @author Alexander Litus
 */
@Slf4j
@SuppressWarnings("UnnecessaryQualifiedReference")
class EnrichmentsFinder {

    private static final String MSG_FQN_REGEX = /[a-zA-Z0-9._]+/

    /**
     * The field number of the field option `by` defined in `Spine/core-java`.
     */
    private static final String OPTION_FIELD_NUMBER_ENRICH_BY = "57125"

    private static final Pattern OPTION_PATTERN_ENRICH_BY =
            Pattern.compile(/$OPTION_FIELD_NUMBER_ENRICH_BY: "($MSG_FQN_REGEX)"/)

    /**
     * The field number of the message option `enrichment_for` defined in `Spine/core-java`.
     */
    private static final String OPTION_FIELD_NUMBER_ENRICHMENT_FOR = "57124"

    private static final Pattern PATTERN_SPACE = Pattern.compile(" ")
    private static final Pattern PATTERN_QUOTE = Pattern.compile("\"")

    private static final String PROTO_TYPE_SEPARATOR = "."
    private static final String EVENT_NAME_SEPARATOR = ","
    private static final String COLON = ":"

    private final FileDescriptorProto file
    private final GString packagePrefix

    /**
     * Creates a new instance.
     *
     * @param file a file to search enrichments in
     */
    EnrichmentsFinder(FileDescriptorProto file) {
        this.file = file
        this.packagePrefix = "${file.getPackage()}$PROTO_TYPE_SEPARATOR"
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
        final Map.Entry<GString, GString> entry = scanMsg(msg)
        if (entry) {
            put(entry, mapBuilder)
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

    private Map.Entry<GString, GString> scanMsg(DescriptorProto msg) {
        final GString eventNames = parseEventNamesFromMsgOption(msg)
        if (!eventNames) {
            return null
        }
        final GString enrichmentName = packagePrefix + msg.getName()
        return new SimpleEntry<>(enrichmentName, eventNames)
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
                    final GString outerEventName = "$packagePrefix${msg.getName()}"
                    final GString enrichmentName = "$outerEventName$PROTO_TYPE_SEPARATOR${innerMsg.getName()}"
                    return new SimpleEntry<>(enrichmentName, outerEventName)
                }
            }
        }
        return null
    }

    private GString parseEventNamesFromMsgOption(DescriptorProto msg) {
        // This needed option is "unknown" and serialized, but it is possible to print option field numbers and values.
        final String optionsStr = msg.getOptions().getUnknownFields().toString().trim()
        if (!optionsStr.contains(OPTION_FIELD_NUMBER_ENRICHMENT_FOR)) {
            return null
        }
        final List<String> eventNamesPrimary = parseEventNames(optionsStr)
        final List<String> eventNamesResult = newLinkedList()
        for (String eventName : eventNamesPrimary) {
            final boolean isFqn = eventName.contains(PROTO_TYPE_SEPARATOR)
            if (isFqn) {
                eventNamesResult.add(eventName)
            } else {
                eventNamesResult.add(packagePrefix + eventName)
            }
        }
        final String result = Joiner.on(EVENT_NAME_SEPARATOR).join(eventNamesResult)
        return "$result"
    }

    private static List<String> parseEventNames(String optionsStr) {
        String opts = optionsStr
        final int colonIndex = opts.indexOf(COLON)
        opts = opts.substring(colonIndex + 1)
        opts = PATTERN_SPACE.matcher(opts).replaceAll("")
        opts = PATTERN_QUOTE.matcher(opts).replaceAll("")
        final List<String> eventNames = ImmutableList.copyOf(opts.split(EVENT_NAME_SEPARATOR))
        return eventNames
    }

    private static boolean hasOptionEnrichBy(FieldDescriptorProto field) {
        final String optionsStr = getOptionsString(field)
        final boolean result = optionsStr.contains(OPTION_FIELD_NUMBER_ENRICH_BY)
        return result
    }

    private static GString parseEventNameFromOptBy(FieldDescriptorProto field) {
        final String optionsStr = getOptionsString(field)
        final Matcher matcher = OPTION_PATTERN_ENRICH_BY.matcher(optionsStr)
        if (!matcher.matches()) {
            return null
        }
        final String fieldFqn = matcher.group(1)
        final int index = fieldFqn.lastIndexOf(PROTO_TYPE_SEPARATOR)
        if (index < 0) {
            return null
        }
        final String eventName = fieldFqn.substring(0, index)
        return "$eventName"
    }

    private static String getOptionsString(FieldDescriptorProto field) {
        // This needed option is "unknown" and serialized, but it is possible to print option field numbers and values.
        final String optionsStr = field.getOptions().getUnknownFields().toString().trim()
        return optionsStr
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
