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

import static org.spine3.gradle.protobuf.failures.JavadocEscaper.EscapedStrings.fromBeginningOf;

/**
 * Utility class for escaping a Javadoc text.
 *
 * @author Dmytro Grankin
 */
@SuppressWarnings("UtilityClass")
class JavadocEscaper {

    private JavadocEscaper() {
    }

    /**
     * Escapes the {@link EscapedStrings} from a Javadoc text.
     *
     * @param javadocText the unescaped Javadoc text
     * @return the escaped Javadoc text
     */
    static String escape(String javadocText) {
        final StringBuilder escapedJavadocBuilder = new StringBuilder(javadocText.length() * 2);

        // If javadocText starts with a slash, it interpreted like a comment ending.
        // To handle this case, we should add "*" before.
        String unescapedPart = '*' + javadocText;
        while (!unescapedPart.isEmpty()) {
            final EscapedStrings escapedString = fromBeginningOf(unescapedPart);

            if (escapedString != null) {
                escapedJavadocBuilder.append(escapedString.getEscaped());
                unescapedPart = unescapedPart.substring(escapedString.getUnescaped()
                                                                     .length());
            } else {
                escapedJavadocBuilder.append(unescapedPart.charAt(0));
                unescapedPart = unescapedPart.substring(1);
            }
        }

        // Remove added "*" in the beginning.
        return escapedJavadocBuilder.toString()
                                    .substring(1);
    }

    enum EscapedStrings {
        COMMENT_BEGINNING("/*", "/&#42;"),
        COMMENT_ENDING("*/", "*&#47;"),
        BACK_SLASH("\\", "&#92;"),
        AT_MARK("@", "&#64;"),
        AMPERSAND("&", "&amp;"),
        LESS_THAN("<", "&lt;"),
        GREATER_THAN(">", "&gt;");

        /**
         * A string that should be avoided in a Javadoc text.
         */
        private final String unescaped;
        private final String escaped;

        EscapedStrings(String unescaped, String escaped) {
            this.unescaped = unescaped;
            this.escaped = escaped;
        }

        static EscapedStrings fromBeginningOf(String unescapedComment) {
            for (EscapedStrings escapedCharacter : values()) {
                if (unescapedComment.startsWith(escapedCharacter.unescaped)) {
                    return escapedCharacter;
                }
            }

            return null;
        }

        String getEscaped() {
            return escaped;
        }

        public String getUnescaped() {
            return unescaped;
        }
    }
}
