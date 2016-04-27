package org.spine3.sample;

import org.spine3.sample.failures.Failure1;

/**
 * Uses generated failures to make sure failures generation passed before compilation.
 */
@SuppressWarnings("unused") // Is used to break compileJava task in case of broken pipeline
public class FailuresUser {

    public void testImport() {
        Class<Failure1> failure1Class = Failure1.class;
    }
}
