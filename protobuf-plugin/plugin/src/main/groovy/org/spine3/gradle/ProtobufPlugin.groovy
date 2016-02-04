package org.spine3.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.spine3.gradle.failures.FailuresGenPlugin
import org.spine3.gradle.lookup.ProtoLookupPlugin

class ProtobufPlugin implements Plugin<Project> {

    @Override
    void apply(Project target) {

        final ProtoLookupPlugin protoLookup = new ProtoLookupPlugin();
        final FailuresGenPlugin failuresGen = new FailuresGenPlugin();

        protoLookup.apply(target);
        failuresGen.apply(target);
    }
}
