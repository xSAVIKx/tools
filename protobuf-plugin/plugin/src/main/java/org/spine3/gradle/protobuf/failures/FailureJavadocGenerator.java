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

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.SourceCodeInfo.Location;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A Generator for the failure Javadocs content.
 *
 * @author Dmytro Grankin
 */
public class FailureJavadocGenerator {

    @SuppressWarnings("HardcodedLineSeparator")
    private static final String LINE_SEPARATOR = "\n";

    private final FailureInfo failureInfo;

    public FailureJavadocGenerator(FailureInfo failureInfo) {
        this.failureInfo = failureInfo;
    }

    //TODO:2017-03-17:dmytro.grankin: escape HTML in Javadoc

    /**
     * Generates a Javadoc content for the failure in the proto compiler style.
     *
     * @return the class-level Javadoc content
     */
    @SuppressWarnings("StringBufferWithoutInitialCapacity") // Cannot make valuable initialization
    public String generateClassJavadoc() {
        final String leadingComments = getLeadingComment();
        final StringBuilder javadoc = new StringBuilder();

        if (leadingComments != null) {
            javadoc.append("<pre>")
                   .append(LINE_SEPARATOR)
                   .append(leadingComments)
                   .append("</pre>")
                   .append(LINE_SEPARATOR)
                   .append(LINE_SEPARATOR);
        }

        javadoc.append("Protobuf type {@code ")
               .append(failureInfo.getJavaPackage())
               .append(failureInfo.getOuterClassName())
               .append('.')
               .append(failureInfo.getClassName())
               .append('}');
        return javadoc.toString();
    }

    private String getLeadingComment() {
        if (!failureInfo.getFile()
                        .hasSourceCodeInfo()) {
            throw new IllegalStateException("Source code info should be enabled");
        }

        final Collection<Integer> path = getMessageLocationPath();
        for (Location location : failureInfo.getFile()
                                            .getSourceCodeInfo()
                                            .getLocationList()) {
            if (location.getPathList()
                        .equals(path)) {
                return location.hasLeadingComments()
                       ? location.getLeadingComments()
                       : null;
            }
        }
        return null;
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

    private int getTopLevelMessageIndex() {
        final List<DescriptorProto> messages = failureInfo.getFile()
                                                          .getMessageTypeList();
        for (DescriptorProto currentMessage : messages) {
            if (currentMessage.equals(failureInfo.getDescriptor())) {
                return messages.indexOf(failureInfo.getDescriptor());
            }
        }

        throw new IllegalStateException("The failure file must contains the failure.");
    }
}
