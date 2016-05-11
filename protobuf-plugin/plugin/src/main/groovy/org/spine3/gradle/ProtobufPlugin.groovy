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
        protoLookup.apply(target);

        final FailuresGenPlugin failuresGen = new FailuresGenPlugin();
        failuresGen.apply(target);

        final EntityLookupPlugin entityLookup = new EntityLookupPlugin();
        entityLookup.apply(target);

        final CleaningPlugin cleaningPlugin = new CleaningPlugin();
        cleaningPlugin.apply(target);
    }
}
