/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
package org.spine3.tools.javadoc.fqnchecker;

import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.spine3.tools.javadoc.fqnchecker.CheckJavadocPlugin.SPINE_LINK_CHECKER_EXTENSION_NAME;

/**
 * A configuration class for the {@link CheckJavadocPlugin}.
 *
 * @author Alexander Aleksandrov
 */
public class Extension {

    private int threshold = 0;

    private String reactionType = "";

    public String getReactionType() {
        return reactionType;
    }
    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        log().debug("Seting up threshold to {}", threshold);
        this.threshold = threshold;
    }

    public void setReactionType(String reactionType) {
        log().debug("Seting up reaction type to {}", reactionType);
        this.reactionType = reactionType;
    }

    public static String getReactionType(Project project) {
        final String reactionType = checkJavadoc(project).reactionType;
        if (reactionType.isEmpty()) {
            return "warn";
        }else {
            return reactionType;
        }
    }

    public static int getThreshold(Project project) {
        final int threshold = checkJavadoc(project).threshold;
        if (threshold < 0) {
            return 0;
        } else {
            return threshold;
        }
    }


    private static Extension checkJavadoc(Project project) {
        return (Extension) project.getExtensions()
                                  .getByName(SPINE_LINK_CHECKER_EXTENSION_NAME);
    }

    private static Logger log() {
        return LoggerSingleton.INSTANCE.logger;
    }

    private enum LoggerSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger logger = LoggerFactory.getLogger(Extension.class);
    }
}
