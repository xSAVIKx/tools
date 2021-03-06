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
package org.spine3.gradle.protobuf.util;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;

import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.emptyMap;

/**
 * A utility class which helps to get "unknown" Protobuf option field numbers
 * (as in option type declaration) and option values.
 *
 * <p>For example, a map with pairs:
 *
 * <p>{@code 50123 -> "option_string_value_1"}
 * <p>{@code 50124 -> "option_string_value_2"}
 *
 * <p>An option is "unknown" and serialized if there is no dependency on the artifact
 * which contains the needed option definition.
 * For example, we should not depend on "Spine/core-java" project artifacts to avoid
 * circular dependency.
 *
 * <p>There can be several option properties, and several same numbers in the options string:
 *
 * [(decimal_max).value = "64.5", (decimal_max).inclusive = true];
 *
 * Currently, we do not need options with several properties.
 * So, only first option property values are obtained, and others are ignored.
 *
 * @author Alexander Litus
 */
public class UnknownOptions {

    private static final Pattern PATTERN_COLON = Pattern.compile(":");
    @SuppressWarnings("HardcodedLineSeparator")
    private static final Pattern PATTERN_NEW_LINE = Pattern.compile("\n");
    private static final String QUOTE = "\"";

    private UnknownOptions() {
    }

    /** Returns a map from "unknown" Protobuf option field numbers to option values. */
    public static Map<Long, String> getUnknownOptions(FileDescriptorProto file) {
        final String optionsStr = file.getOptions()
                                      .getUnknownFields()
                                      .toString()
                                      .trim();
        final Map<Long, String> result = parseOptions(optionsStr);
        return result;
    }

    /**
     * Returns a string value of "unknown" Protobuf option or {@code null} if no option
     * with such field number found.
     */
    public static String getUnknownOptionValue(FileDescriptorProto file, Long optionFieldNumber) {
        final Map<Long, String> options = getUnknownOptions(file);
        final String result = options.get(optionFieldNumber);
        return result;
    }

    /** Returns a map from "unknown" Protobuf option field numbers to option values. */
    public static Map<Long, String> getUnknownOptions(DescriptorProto message) {
        final String optionsStr = message.getOptions()
                                         .getUnknownFields()
                                         .toString()
                                         .trim();
        final Map<Long, String> result = parseOptions(optionsStr);
        return result;
    }

    /**
     * Returns a string value of "unknown" Protobuf option or {@code null} if no option
     * with such field number found.
     */
    public static String getUnknownOptionValue(DescriptorProto msg, Long optionFieldNumber) {
        final Map<Long, String> options = getUnknownOptions(msg);
        final String result = options.get(optionFieldNumber);
        return result;
    }

    /** Returns a map from "unknown" Protobuf option field numbers to option values. */
    public static Map<Long, String> getUnknownOptions(FieldDescriptorProto field) {
        final String optionsStr = field.getOptions()
                                       .getUnknownFields()
                                       .toString()
                                       .trim();
        final Map<Long, String> result = parseOptions(optionsStr);
        return result;
    }

    /**
     * Returns {@code true} if the field is marked with an option which has the given field number
     * in its definition.
     */
    public static boolean hasUnknownOption(FieldDescriptorProto field, Long optionFieldNumber) {
        final String optionsStr = field.getOptions()
                                       .getUnknownFields()
                                       .toString()
                                       .trim();
        final boolean result = optionsStr.contains(String.valueOf(optionFieldNumber));
        return result;
    }

    /**
     * Returns a string value of "unknown" Protobuf option or {@code null} if no option with
     * such field number found.
     */
    public static String getUnknownOptionValue(FieldDescriptorProto field, Long optionFieldNumber) {
        final Map<Long, String> options = getUnknownOptions(field);
        final String result = options.get(optionFieldNumber);
        return result;
    }

    private static Map<Long, String> parseOptions(String optionsStr) {
        if (optionsStr.trim()
                      .isEmpty()) {
            return emptyMap();
        }
        final Map<Long, String> map = newHashMap();
        final String[] options = PATTERN_NEW_LINE.split(optionsStr);
        for (String option : options) {
            parseAndPutNumberAndValue(option, map);
        }
        return ImmutableMap.copyOf(map);
    }

    private static void parseAndPutNumberAndValue(String option, Map<Long, String> map) {
        // we need only two parts split by the first colon
        final int limit = 2;
        final String[] numberAndValue = PATTERN_COLON.split(option, limit);
        final String numberStr = numberAndValue[0].trim();
        final Long number = Long.valueOf(numberStr);
        String value = numberAndValue[1].trim();
        if (value.startsWith(QUOTE) && value.endsWith(QUOTE)) {
            value = value.substring(1, value.length() - 1);
        }
        // Check for duplicates to avoid exceptions on immutable map creation.
        // See the class docs for details.
        if (!map.containsKey(number)) {
            map.put(number, value);
        }
    }

}
