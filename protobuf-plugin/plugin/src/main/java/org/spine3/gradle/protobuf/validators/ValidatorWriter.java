package org.spine3.gradle.protobuf.validators;

import com.google.protobuf.Descriptors;

/**
 * Class, which writes Validator java code, based on it's descriptor.
 *
 * @author Illia Shepilov
 */
public class ValidatorWriter {

    private final Descriptors.Descriptor message;

    public ValidatorWriter(Descriptors.Descriptor message) {
        this.message = message;
    }

    public void write() {

    }
}
