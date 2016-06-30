package org.spine3.gradle
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.spine3.gradle.cleaning.CleaningPlugin
import org.spine3.gradle.failures.FailuresGenPlugin
import org.spine3.gradle.lookup.enrichments.EnrichmentLookupPlugin
import org.spine3.gradle.lookup.entity.EntityLookupPlugin
import org.spine3.gradle.lookup.proto.ProtoToJavaMapperPlugin
/**
 * Root plugin, which aggregates other plugins.
 */
class ProtobufPlugin implements Plugin<Project> {

    @Override
    void apply(Project target) {
        new CleaningPlugin().apply(target);
        new ProtoToJavaMapperPlugin().apply(target);
        new EntityLookupPlugin().apply(target);
        new EnrichmentLookupPlugin().apply(target);
        new FailuresGenPlugin().apply(target);
    }
}
