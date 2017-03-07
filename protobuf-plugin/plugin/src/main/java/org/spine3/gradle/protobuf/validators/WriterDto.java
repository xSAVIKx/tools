package org.spine3.gradle.protobuf.validators;

import com.google.common.base.Objects;
import com.google.protobuf.DescriptorProtos.DescriptorProto;

/**
 * @author Illia Shepilov
 */
class WriterDto {

    private final String javaPackage;
    private final String javaClass;
    private final DescriptorProto msgDescriptor;

    WriterDto(String javaPackage, String javaClass, DescriptorProto msgDescriptor) {
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
        WriterDto dto = (WriterDto) o;
        return Objects.equal(javaPackage, dto.javaPackage) &&
                Objects.equal(javaClass, dto.javaClass) &&
                Objects.equal(msgDescriptor, dto.msgDescriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(javaPackage, javaClass, msgDescriptor);
    }
}
