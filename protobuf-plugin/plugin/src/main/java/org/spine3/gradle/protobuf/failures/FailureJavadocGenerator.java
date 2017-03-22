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

package org.spine3.gradle.protobuf.failures;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.lang.System.lineSeparator;

/**
 * A generator for the failure Javadocs content.
 *
 * <p>Could be used only if protobuf plugin configured properly:
 * <pre> {@code
 * generateProtoTasks {
 *     all().each { final task ->
 *         // If true, the descriptor set will contain line number information
 *         // and comments. Default is false.
 *         task.descriptorSetOptions.includeSourceInfo = true
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * @author Dmytro Grankin
 * @see <a href="https://github.com/google/protobuf-gradle-plugin/blob/master/README.md#generate-descriptor-set-files">
 * Protobuf plugin configuration</a>
 */
public class FailureJavadocGenerator {

    @VisibleForTesting
    protected static final String OPENING_PRE = "<pre>";

    private final FailureMetadata failureMetadata;

    public FailureJavadocGenerator(FailureMetadata failureMetadata) {
        this.failureMetadata = failureMetadata;
    }

    /**
     * Generates a Javadoc content for the failure.
     *
     * @return the class-level Javadoc content
     */
    @SuppressWarnings("StringBufferWithoutInitialCapacity") // Cannot make valuable initialization
    public String generateClassJavadoc() {
        final String leadingComments = getFailureLeadingComments();
        final StringBuilder builder = new StringBuilder();

        if (leadingComments != null) {
            builder.append(OPENING_PRE)
                   .append(lineSeparator())
                   .append(escapeJavadoc(leadingComments))
                   .append("</pre>")
                   .append(lineSeparator())
                   .append(lineSeparator());
        }

        builder.append("Failure based on protobuf type {@code ")
               .append(failureMetadata.getJavaPackage())
               .append('.')
               .append(failureMetadata.getClassName())
               .append('}')
               .append(lineSeparator());
        return builder.toString();
    }

    /**
     * Generates a Javadoc content for the failure constructor.
     *
     * @return the constructor Javadoc content
     */
    public String generateConstructorJavadoc() {
        final StringBuilder builder = new StringBuilder("Creates a new instance.");
        final int maxFieldLength = getMaxFieldNameLength();

        builder.append(lineSeparator())
               .append(lineSeparator());
        for (FieldDescriptorProto field : failureMetadata.getDescriptor()
                                                         .getFieldList()) {
            final String leadingComments = getFieldLeadingComments(field);
            final String fieldName = field.getName();
            final int commentOffset = maxFieldLength - fieldName.length();
            if (leadingComments != null) {
                builder.append("@param ")
                       .append(fieldName)
                       .append(Strings.repeat(" ", commentOffset))
                       .append(escapeJavadoc(leadingComments));
            }
        }

        return builder.toString();
    }

    private String getFieldLeadingComments(FieldDescriptorProto field) {
        final Collection<Integer> fieldPath = getFieldLocationPath(field);
        return getLeadingComments(fieldPath);
    }

    private String getFailureLeadingComments() {
        final Collection<Integer> path = getMessageLocationPath();
        return getLeadingComments(path);
    }

    private String getLeadingComments(Collection<Integer> path) {
        if (!failureMetadata.getFileDescriptor()
                            .hasSourceCodeInfo()) {
            throw new IllegalStateException("Source code info should be enabled");
        }

        final Location location = getLocation(path);
        return location != null && location.hasLeadingComments()
               ? location.getLeadingComments()
               : null;
    }

    /**
     * Returns the message location path for a top-level message definition.
     *
     * <p>Path for nested messages formed in different way.
     *
     * @return the message location path
     */
    private Collection<Integer> getMessageLocationPath() {
        return Arrays.asList(
                FileDescriptorProto.MESSAGE_TYPE_FIELD_NUMBER,
                getTopLevelMessageIndex()
        );
    }

    /**
     * Returns the field location path.
     *
     * <p>Extensions are not supported.
     *
     * @param field the field to get location path
     * @return the field location path
     */
    private Collection<Integer> getFieldLocationPath(FieldDescriptorProto field) {
        final Collection<Integer> path = new ArrayList<>();

        path.addAll(getMessageLocationPath());
        path.add(DescriptorProto.FIELD_FIELD_NUMBER);
        path.add(getFieldIndex(field));
        return path;
    }

    private int getTopLevelMessageIndex() {
        final List<DescriptorProto> messages = failureMetadata.getFileDescriptor()
                                                              .getMessageTypeList();
        for (DescriptorProto currentMessage : messages) {
            if (currentMessage.equals(failureMetadata.getDescriptor())) {
                return messages.indexOf(failureMetadata.getDescriptor());
            }
        }

        throw new IllegalStateException("The failure file must contains the failure.");
    }

    private int getFieldIndex(FieldDescriptorProto field) {
        return failureMetadata.getDescriptor()
                              .getFieldList()
                              .indexOf(field);
    }

    private Location getLocation(Collection<Integer> path) {
        for (Location location : failureMetadata.getFileDescriptor()
                                                .getSourceCodeInfo()
                                                .getLocationList()) {
            if (location.getPathList()
                        .equals(path)) {
                return location;
            }
        }

        return null;
    }

    private int getMaxFieldNameLength() {
        final Ordering<FieldDescriptorProto> ordering = new Ordering<FieldDescriptorProto>() {
            @SuppressWarnings("ConstantConditions") // getName() never returns null.
            @Override
            public int compare(@Nullable FieldDescriptorProto left, @Nullable FieldDescriptorProto right) {
                return Ints.compare(left.getName()
                                        .length(), right.getName()
                                                        .length());
            }
        };
        final FieldDescriptorProto longestNameField = ordering.max(failureMetadata.getDescriptor()
                                                                                  .getFieldList());
        return longestNameField.getName()
                               .length();
    }

    @VisibleForTesting
    protected static String escapeJavadoc(CharSequence javadoc) {
        final StringBuilder builder = new StringBuilder(javadoc.length() * 2);

        char previous = '*';
        for (int i = 0; i < javadoc.length(); i++) {
            final char current = javadoc.charAt(i);
            builder.append(EscapedCharacters.escape(current, previous));
            previous = current;
        }

        return builder.toString();
    }

    enum EscapedCharacters {
        ASTERISK('*', "&#42;"),     // Only "*/" should be avoided.
        SLASH('/', "&#47;"),        // Only "/*" should be avoided.
        BACK_SLASH('\\', "&#92;"),
        AT_MARK('@', "&#64;"),
        AMPERSAND('&', "&amp;"),
        LESS_THAN('<', "&lt;"),
        GREATER_THAN('>', "&gt;");

        /**
         * A character that should be avoided in a Javadoc text.
         */
        private final char unescapedCharacter;
        private final String escapedString;

        EscapedCharacters(char unescapedCharacter, String escapedString) {
            this.unescapedCharacter = unescapedCharacter;
            this.escapedString = escapedString;
        }

        String getEscapedString() {
            return escapedString;
        }

        private static String escape(char current, char previous) {
            for (EscapedCharacters escapedSymbol : EscapedCharacters.values()) {
                if (escapedSymbol.unescapedCharacter == current) {
                    if (isAsteriskNotCommentBeginningPart(escapedSymbol, previous)
                            || isSlashNotCommentEndingPart(escapedSymbol, previous)) {
                        return String.valueOf(current);
                    }
                    return escapedSymbol.escapedString;
                }
            }
            return String.valueOf(current);
        }

        private static boolean isAsteriskNotCommentBeginningPart(EscapedCharacters current,
                                                                 char previous) {
            return current == ASTERISK && previous != SLASH.unescapedCharacter;
        }

        private static boolean isSlashNotCommentEndingPart(EscapedCharacters current, char previous) {
            return current == SLASH && previous != ASTERISK.unescapedCharacter;
        }
    }
}
