package org.spine3.gradle;

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.spine3.gradle.cleaning.CleaningPlugin
import org.spine3.gradle.failures.FailuresGenPlugin
import org.spine3.gradle.lookup.entity.EntityLookupPlugin
import org.spine3.gradle.lookup.proto.ProtoToJavaMapperPlugin

/**
 * Root plugin, which aggregates other plugins.
 */
class ProtobufPlugin implements Plugin<Project> {

    @Override
    void apply(Project target) {

        final ProtoToJavaMapperPlugin protoLookup = new ProtoToJavaMapperPlugin();
        final FailuresGenPlugin failuresGen = new FailuresGenPlugin();
        final EntityLookupPlugin entityLookup = new EntityLookupPlugin();
        final CleaningPlugin cleaningPlugin = new CleaningPlugin();

        protoLookup.apply(target);
        entityLookup.apply(target);
        failuresGen.apply(target);
        cleaningPlugin.apply(target);
    }
}
