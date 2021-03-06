/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.spine3.gradle.protobuf;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.gradle.protobuf.cleaning.CleaningPlugin;
import org.spine3.gradle.protobuf.failure.FailuresGenPlugin;
import org.spine3.gradle.protobuf.lookup.enrichments.EnrichmentLookupPlugin;
import org.spine3.gradle.protobuf.lookup.proto.ProtoToJavaMapperPlugin;

/**
 * @author Alexander Litus
 * @author Mikhail Mikhaylov
 */
public class ProtobufPlugin implements Plugin<Project> {

    public static final String SPINE_PROTOBUF_EXTENSION_NAME = "spineProtobuf";

    @Override
    public void apply(Project project) {
        log().debug("Adding the extension to the project");
        project.getExtensions()
               .create(SPINE_PROTOBUF_EXTENSION_NAME, Extension.class);

        log().debug("Applying Spine cleaning plugin");
        new CleaningPlugin().apply(project);

        log().debug("Applying Spine proto-to-java mapper plugin");
        new ProtoToJavaMapperPlugin().apply(project);

        log().debug("Applying Spine enrichment lookup plugin");
        new EnrichmentLookupPlugin().apply(project);

        log().debug("Applying Spine failures generation plugin");
        new FailuresGenPlugin().apply(project);
    }

    private static Logger log() {
        return LoggerSingleton.INSTANCE.logger;
    }

    private enum LoggerSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger logger = LoggerFactory.getLogger(ProtobufPlugin.class);
    }
}
