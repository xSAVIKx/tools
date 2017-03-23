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

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

import static org.spine3.gradle.protobuf.failures.JavadocEscaper.EscapedCharacters.AMPERSAND;
import static org.spine3.gradle.protobuf.failures.JavadocEscaper.EscapedCharacters.ASTERISK;
import static org.spine3.gradle.protobuf.failures.JavadocEscaper.EscapedCharacters.AT_MARK;
import static org.spine3.gradle.protobuf.failures.JavadocEscaper.EscapedCharacters.BACK_SLASH;
import static org.spine3.gradle.protobuf.failures.JavadocEscaper.EscapedCharacters.GREATER_THAN;
import static org.spine3.gradle.protobuf.failures.JavadocEscaper.EscapedCharacters.LESS_THAN;
import static org.spine3.gradle.protobuf.failures.JavadocEscaper.EscapedCharacters.SLASH;

/**
 * Utility class for escaping a Javadoc text.
 *
 * @author Dmytro Grankin
 */
@SuppressWarnings("UtilityClass")
class JavadocEscaper {

    /**
     * Serves for escaping a Javadoc text without the exclusions.
     */
    private static final Escaper BASE_ESCAPER;

    static {
        BASE_ESCAPER = Escapers.builder()
                               .addEscape(ASTERISK.unescapedCharacter, ASTERISK.escapedString)
                               .addEscape(SLASH.unescapedCharacter, SLASH.escapedString)
                               .addEscape(BACK_SLASH.unescapedCharacter, BACK_SLASH.escapedString)
                               .addEscape(AT_MARK.unescapedCharacter, AT_MARK.escapedString)
                               .addEscape(AMPERSAND.unescapedCharacter, AMPERSAND.escapedString)
                               .addEscape(LESS_THAN.unescapedCharacter, LESS_THAN.escapedString)
                               .addEscape(GREATER_THAN.unescapedCharacter, GREATER_THAN.escapedString)
                               .build();
    }

    private JavadocEscaper() {
    }

    /**
     * Escapes the {@link EscapedCharacters} from a Javadoc text.
     *
     * <p>There are two exclusions, that should not be escaped:
     * <ul>
     * <li>an asterisk if is not part of an opening comment;</li>
     * <li>a slash if is not part of an ending comment.</li>
     * </ul>
     *
     * @param javadocText the unescaped Javadoc text
     * @return the escaped Javadoc text
     */
    static String escape(String javadocText) {
        final StringBuilder builder = new StringBuilder(javadocText.length() * 2);
        final String fullyEscaped = BASE_ESCAPER.escape(javadocText);

        char previous = ASTERISK.unescapedCharacter;
        for (int i = 0; i < fullyEscaped.length(); ) {

            // Unescape slash if is not a part of "*/"
            if (fullyEscaped.startsWith(SLASH.escapedString, i)
                    && previous != ASTERISK.unescapedCharacter) {
                previous = SLASH.unescapedCharacter;
                i += SLASH.escapedString.length();

                // Unescape asterisk if is not a part of "/*"
            } else if (fullyEscaped.startsWith(ASTERISK.escapedString, i)
                    && previous != SLASH.unescapedCharacter) {
                previous = ASTERISK.unescapedCharacter;
                i += ASTERISK.escapedString.length();
            } else {
                previous = fullyEscaped.charAt(i);
                i++;
            }

            builder.append(previous);
        }

        return builder.toString();
    }

    enum EscapedCharacters {
        ASTERISK('*', "&#42;"),
        SLASH('/', "&#47;"),
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
    }
}
