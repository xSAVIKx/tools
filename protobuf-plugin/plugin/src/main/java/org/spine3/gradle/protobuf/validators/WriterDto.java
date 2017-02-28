package org.spine3.gradle.protobuf.validators;

import com.google.protobuf.DescriptorProtos;

/**
 * @author Illia Shepilov
 */
public class WriterDto {

    private final String javaPackage;
    private final String javaClass;
    private final DescriptorProtos.DescriptorProto msgDescriptor;

    public WriterDto(String javaPackage, String javaClass, DescriptorProtos.DescriptorProto msgDescriptor) {
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

    public DescriptorProtos.DescriptorProto getMsgDescriptor() {
        return msgDescriptor;
    }
}
