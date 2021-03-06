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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * This enum states two behavior types that either log warnings or fail build process.
 *
 * @author Alexander Aleksandrov
 */
public enum Response {
    /**
     * This instance will log warning message.
     */
    WARN("warn"),

    /**
     * This instance will log warning message and then will throw an
     * exception and fail a build process.
     */
    ERROR("error"){
        @Override
        public void logOrFail(Path path) {
            super.logOrFail(path);
            throw new InvalidFqnUsageException(path.toFile()
                                                   .getAbsolutePath(), message);
        }
    };

    private final String responseType;
    private static final String message =
            "Links with fully-qualified names should be in format {@link <FQN> <text>}" +
            " or {@linkplain <FQN> <text>}.";

    Response(String responseType) {
        this.responseType = responseType;
    }

    public String getValue() {
        return responseType;
    }

    /**
     * Logs error message.
     *
     * @param path target path to the file under check.
     */
    public void logOrFail(Path path){
        log().error(message);
    }

    private static Logger log() {
        return Response.LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(Response.class);
    }
}
