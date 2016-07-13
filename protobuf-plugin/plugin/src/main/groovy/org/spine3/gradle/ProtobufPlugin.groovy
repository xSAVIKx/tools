package org.spine3.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.spine3.gradle.cleaning.CleaningPlugin
import org.spine3.gradle.failures.FailuresGenPlugin
import org.spine3.gradle.lookup.enrichments.EnrichmentLookupPlugin
import org.spine3.gradle.lookup.proto.ProtoToJavaMapperPlugin
/**
 * Root plugin, which aggregates other plugins.
 */
class ProtobufPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create("spineProtobuf", Extension.class)
        new CleaningPlugin().apply(project)
        new ProtoToJavaMapperPlugin().apply(project)
        new EnrichmentLookupPlugin().apply(project)
        new FailuresGenPlugin().apply(project)
    }
}
