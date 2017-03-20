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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import java.nio.file.Path;
import java.util.List;

/**
 * Utility class to save and address results of fully qualified name javadoc check.
 *
 * @author Alexander Aleksandrov
 */
public class CheckResultStorage {

    private static ImmutableMap<Path, List<Optional<InvalidFqnUsage>>> resultStorage =
            ImmutableMap.of();

//    private CheckResultStorage(){
//    }

    public ImmutableMap<Path, List<Optional<InvalidFqnUsage>>> getResults() {
        return resultStorage;
    }

    /**
     * Add a new record to storage if it is already exist or creates a new one in case if it's not.
     *
     * @param path file path that contain wrong fomated links
     * @param list list of invalid fully qualified names usages
     */
    public void save(Path path, List<Optional<InvalidFqnUsage>> list) {
        if (resultStorage.isEmpty()) {
            create(path, list);
        } else {
            add(path, list);
        }
    }

    private static void create(Path path, List<Optional<InvalidFqnUsage>> list) {
        resultStorage = new ImmutableMap.Builder<Path, List<Optional<InvalidFqnUsage>>>()
                .put(path, list)
                .build();
    }

    private static void add(Path path, List<Optional<InvalidFqnUsage>> list) {
        resultStorage = new ImmutableMap.Builder<Path, List<Optional<InvalidFqnUsage>>>()
                .putAll(resultStorage)
                .put(path, list)
                .build();
    }

}
