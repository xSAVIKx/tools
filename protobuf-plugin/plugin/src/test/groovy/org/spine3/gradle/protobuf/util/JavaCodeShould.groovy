package org.spine3.gradle.protobuf.util

import org.junit.Test

import static org.junit.Assert.assertEquals

@SuppressWarnings("GroovyInstanceMethodNamingConvention")
class JavaCodeShould {

    @Test
    void calculate_outer_class_name() {
        assertEquals('Failures', JavaCode.toCamelCase('failures'));
        assertEquals('ManyFailures', JavaCode.toCamelCase('many_failures'));
        assertEquals('ManyMoreFailures', JavaCode.toCamelCase('many_more_failures'));
    }
}
