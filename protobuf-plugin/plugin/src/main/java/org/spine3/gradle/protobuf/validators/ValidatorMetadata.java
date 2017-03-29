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

package org.spine3.gradle.protobuf.validators;

import com.google.common.base.Objects;
import com.google.protobuf.DescriptorProtos.DescriptorProto;

/**
 * A metadata for the validator builders.
 *
 * @author Illia Shepilov
 */
public class ValidatorMetadata {

    private final String javaClass;
    private final String javaPackage;
    private final DescriptorProto msgDescriptor;

    ValidatorMetadata(String javaPackage, String javaClass, DescriptorProto msgDescriptor) {
        this.javaPackage = javaPackage;
        this.javaClass = javaClass;
        this.msgDescriptor = msgDescriptor;
    }

    public String getJavaPackage() {
        return javaPackage;
    }

    public String getJavaClass() {
        return javaClass;
    }

    public DescriptorProto getMsgDescriptor() {
        return msgDescriptor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ValidatorMetadata dto = (ValidatorMetadata) o;
        return Objects.equal(javaPackage, dto.javaPackage) &&
                Objects.equal(javaClass, dto.javaClass) &&
                Objects.equal(msgDescriptor, dto.msgDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(javaPackage, javaClass, msgDescriptor);
    }
}
