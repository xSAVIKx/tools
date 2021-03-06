/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package org.spine3.gradle.protobuf.lookup.enrichments;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static org.spine3.gradle.protobuf.util.UnknownOptions.getUnknownOptionValue;
import static org.spine3.gradle.protobuf.util.UnknownOptions.hasUnknownOption;

/**
 * Finds event enrichment Protobuf definitions.
 *
 * @author Alexander Litus
 * @author Alex Tymchenko
 */
class EnrichmentsFinder {

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
    private static final String EMPTY_TYPE_NAME = "";

    /**
     * Wildcard option used in By field option.
     *
     * <p>{@code string enrichment_value [(by) = "*.my_event_id"];} tells that this enrichment
     * may have any target event types. That's why an FQN of the target type is replaced by
     * this wildcard option.
     */
    private static final String ANY_BY_OPTION_TARGET = "*";
    private static final String PIPE_SEPARATOR = "|";
    private static final Pattern PATTERN_SPACE = compile(" ");
    private static final Pattern PATTERN_TARGET_NAME_SEPARATOR = compile(TARGET_NAME_SEPARATOR);
    private static final Pattern PATTERN_PIPE_SEPARATOR = compile("\\|");

    private final FileDescriptorProto file;
    private final String packagePrefix;

    /**
     * Creates a new instance.
     *
     * @param file a file to search enrichments in
     */
    EnrichmentsFinder(FileDescriptorProto file) {
        this.file = file;
        this.packagePrefix = file.getPackage() + PROTO_TYPE_SEPARATOR;
    }

    /**
     * Finds event enrichment Protobuf definitions in the file.
     *
     * @return a map from enrichment type name to event to enrich type name
     */
    Map<String, String> findEnrichments() {
        log().debug("Looking up for the enrichments in {}", file.getName());
        final HashMultimap<String, String> result = HashMultimap.create();
        final List<DescriptorProto> messages = file.getMessageTypeList();
        for (DescriptorProto msg : messages) {
            putEntry(result, msg);
        }
        log().debug("Found enrichments: {}", result.toString());
        return mergeDuplicateValues(result);
    }

    /**
     * Merge duplicate values into a single value for the same key.
     *
     * <p>The values are joined with {@link EnrichmentsFinder#TARGET_NAME_SEPARATOR}.
     *
     * <p>Merging may be required when the wildcard `By` option values are handled,
     * i.e. when processing a single enrichment type as a map key, but multiple target
     * event types as values.
     */
    private static Map<String, String> mergeDuplicateValues(HashMultimap<String, String> source) {
        log().debug("Merging duplicate properties in enrichments.proto");
        final ImmutableMap.Builder<String, String> mergedResult = ImmutableMap.builder();
        for (String key : source.keySet()) {
            final Set<String> valuesPerKey = source.get(key);
            // Empty type name might be present in the values
            // If so, remove it from the set
            valuesPerKey.remove(EMPTY_TYPE_NAME);

            final String mergedValue;
            if (valuesPerKey.size() > 1) {
                mergedValue = Joiner.on(TARGET_NAME_SEPARATOR)
                                    .join(valuesPerKey);
            } else {
                mergedValue = valuesPerKey.iterator()
                                          .next();
            }
            mergedResult.put(key, mergedValue);
        }

        return mergedResult.build();
    }

    private void putEntry(Multimap<String, String> targetMap, DescriptorProto msg) {
        final Map<String, String> entries = scanMsg(msg);
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            put(entry, targetMap);
        }
        if (!entries.isEmpty()) {
            return;
        }
        final Map<String, String> entryFromField = scanFields(msg);
        if (entryFromField.size() > 0) {
            for (Map.Entry<String, String> entry : entryFromField.entrySet()) {
                put(entry, targetMap);
            }
            return;
        }
        final Map.Entry<String, String> entryFromInnerMsg = scanInnerMessages(msg);
        if (entryFromInnerMsg != null) {
            put(entryFromInnerMsg, targetMap);
            log().debug("Found enrichment: {} -> {}",
                        entryFromInnerMsg.getKey(),
                        entryFromInnerMsg.getValue());
        } else {
            log().debug("No enrichment or event annotations found for message {}", msg.getName());
        }
    }

    @SuppressWarnings("MethodWithMoreThanThreeNegations")
    private Map<String, String> scanMsg(DescriptorProto msg) {
        final ImmutableMap.Builder<String, String> msgScanResultBuilder = ImmutableMap.builder();
        final String messageName = packagePrefix + msg.getName();

        // Treating current {@code msg} as an enrichment object.
        log().debug("Scanning message {} for the enrichment annotations", messageName);
        final String eventNames = parseEventNamesFromMsgOption(msg);
        if (eventNames != null && !eventNames.isEmpty()) {
            log().debug("Found target events: {}", eventNames);
            msgScanResultBuilder.put(messageName, eventNames);
        } else {
            log().debug("No target events found");
        }

        // Treating current {@code msg} as a target for enrichment (e.g. Spine event).
        log().debug("Scanning message {} for the enrichment target annotations", messageName);
        final Collection<String> enrichmentNames = parseEnrichmentNamesFromMsgOption(msg);
        if (enrichmentNames != null && !enrichmentNames.isEmpty()) {
            log().debug("Found enrichments for event {}: {}", messageName, enrichmentNames);
            for (String enrichmentName : enrichmentNames) {
                msgScanResultBuilder.put(enrichmentName, messageName);
            }
        } else {
            log().debug("No enrichments for event {} found", messageName);
        }

        return msgScanResultBuilder.build();
    }

    private Map<String, String> scanFields(DescriptorProto msg) {
        final String msgName = msg.getName();
        log().debug("Scanning fields of message {} for the enrichment annotations", msgName);
        final Map<String, String> enrichmentsMap = new HashMap<>();
        for (FieldDescriptorProto field : msg.getFieldList()) {
            if (hasOptionEnrichBy(field)) {
                final Collection<String> eventNamesFromBy = parseEventNameFromOptBy(field);
                final Map.Entry<String, String> foundEvents = groupFoundEvents(msgName,
                                                                               eventNamesFromBy,
                                                                               field.getName());
                enrichmentsMap.put(foundEvents.getKey(), foundEvents.getValue());
            }
        }
        return enrichmentsMap;
    }

    private Map.Entry<String, String> groupFoundEvents(String enrichment,
                                                       Collection<String> events,
                                                       String fieldName) {
        final Collection<String> eventGroup = new HashSet<>(events.size());
        for (String eventName : events) {
            if (eventName == null || eventName.trim()
                                              .isEmpty()) {
                throw invalidByOptionValue(enrichment);
            }
            log().debug("'by' option found on field {} targeting {}", fieldName, eventName);

            if (ANY_BY_OPTION_TARGET.equals(eventName)) {
                log().debug("Skipping a wildcard event");
                // Ignore the wildcard By options, as we don't know
                // the target event type in this case.
                continue;
            }
            eventGroup.add(eventName);
        }
        final String enrichmentName = packagePrefix + enrichment;
        final String eventGroupString = Joiner.on(TARGET_NAME_SEPARATOR)
                                              .join(eventGroup);
        final Map.Entry<String, String> result =
                new AbstractMap.SimpleEntry<>(enrichmentName, eventGroupString);

        return result;
    }

    @SuppressWarnings("MethodWithMultipleLoops")    // It's fine in this case.
    private Map.Entry<String, String> scanInnerMessages(DescriptorProto msg) {
        log().debug("Scanning inner messages of {} message for the annotations", msg.getName());
        for (DescriptorProto innerMsg : msg.getNestedTypeList()) {
            for (FieldDescriptorProto field : innerMsg.getFieldList()) {
                if (hasOptionEnrichBy(field)) {
                    final String outerEventName = packagePrefix + msg.getName();
                    final String enrichmentName = outerEventName +
                            PROTO_TYPE_SEPARATOR + innerMsg.getName();
                    log().debug("'by' option found on field {} targeting outer event {}",
                                field.getName(),
                                outerEventName);
                    return new AbstractMap.SimpleEntry<>(enrichmentName, outerEventName);
                }
            }
        }
        return null;
    }

    private String parseEventNamesFromMsgOption(DescriptorProto msg) {
        final String eventNamesStr = getUnknownOptionValue(msg, OPTION_NUMBER_ENRICHMENT_FOR);
        if (eventNamesStr == null) {
            return null;
        }
        final Collection<String> eventNamesResult = normalizeTypeNames(eventNamesStr);
        return Joiner.on(TARGET_NAME_SEPARATOR)
                     .join(eventNamesResult);
    }

    @SuppressWarnings("InstanceMethodNamingConvention")
    // It's important to keep naming self-explanatory.
    private Collection<String> parseEnrichmentNamesFromMsgOption(DescriptorProto msg) {
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
            checkState(!typeName.trim()
                                .isEmpty(), "Empty type name");
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
        final String names = PATTERN_SPACE.matcher(targetNames)
                                          .replaceAll("");
        final String[] namesArray = PATTERN_TARGET_NAME_SEPARATOR.split(names);
        return ImmutableList.copyOf(namesArray);
    }

    private static boolean hasOptionEnrichBy(FieldDescriptorProto field) {
        return hasUnknownOption(field, OPTION_NUMBER_ENRICH_BY);
    }

    /**
     * @return {@link Collection} of strings representing the fully qualified names of
     * the target events for given field
     */
    @SuppressWarnings("IndexOfReplaceableByContains") // On performance purposes
    private static Collection<String> parseEventNameFromOptBy(FieldDescriptorProto field) {
        final String byArgument = getUnknownOptionValue(field, OPTION_NUMBER_ENRICH_BY);
        final String[] fieldFqnsArray;

        if (byArgument.indexOf(PIPE_SEPARATOR) < 0) {
            fieldFqnsArray = new String[]{byArgument};
        } else {
            fieldFqnsArray = PATTERN_PIPE_SEPARATOR.split(byArgument);
        }
        final Collection<String> result = new LinkedList<>();

        for (String fieldFqn : fieldFqnsArray) {
            if (fieldFqn == null) {
                throw invalidByOptionValue(field.getName());
            }
            if (fieldFqn.startsWith(ANY_BY_OPTION_TARGET) && fieldFqnsArray.length > 1) {
                // Multiple argument `by` annotation can not contain wildcard reference onto
                // the event type if the type was not specified with a `enrichment_for` annotation
                throw invalidByOptionUsage(field.getName());
            }
            final int index = fieldFqn.lastIndexOf(PROTO_TYPE_SEPARATOR);
            if (index < 0) {
                // The short form type names are handled as inner types
                continue;
            }
            final String typeFqn = fieldFqn.substring(0, index)
                                           .trim();
            checkState(!typeFqn.isEmpty(), format("Error parsing `by` annotation for field %s",
                                                  field.getName()));
            result.add(typeFqn);
        }

        return result;
    }

    private static void put(Map.Entry<String, String> entry, Multimap<String, String> targetMap) {
        // Put key and value separately to avoid an error.
        targetMap.put(entry.getKey(), entry.getValue());
    }

    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static IllegalStateException invalidByOptionValue(String msgName) {
        throw new IllegalStateException(
                "Field of message `" + msgName + "` has invalid 'by' option value, " +
                        "which must be a fully-qualified field reference.");
    }

    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static IllegalStateException invalidByOptionUsage(String msgName) {
        throw new IllegalStateException(
                "Field of message `" + msgName + "` has invalid 'by' option value. " +
                        "Wildcard type is not allowed with multiple arguments. " +
                        "Please, specify the type either with `by` or " +
                        "with `enrichment_for` annotation.");
    }

    private static Logger log() {
        return LoggerSingleton.INSTANCE.logger;
    }

    private enum LoggerSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger logger = LoggerFactory.getLogger(EnrichmentsFinder.class);
    }
}
