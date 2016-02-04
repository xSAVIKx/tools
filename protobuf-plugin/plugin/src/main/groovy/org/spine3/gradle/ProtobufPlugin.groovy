package org.spine3.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.spine3.gradle.failures.FailuresGenPlugin
import org.spine3.gradle.lookup.ProtoLookupPlugin
import org.spine3.gradle.validation.command.CommandValidationPlugin

class ProtobufPlugin implements Plugin<Project> {

    @Override
    void apply(Project target) {

        final ProtoLookupPlugin protoLookup = new ProtoLookupPlugin();
        final FailuresGenPlugin failuresGen = new FailuresGenPlugin();
        final CommandValidationPlugin commandValidationPlugin = new CommandValidationPlugin();

        protoLookup.apply(target);
        failuresGen.apply(target);
        commandValidationPlugin.apply(target);
    }
}
