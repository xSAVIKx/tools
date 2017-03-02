package org.spine3.gradle.javadoc.fqnchecker;

import com.google.common.base.Optional;
import org.gradle.internal.impldep.com.amazonaws.util.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

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

public class FqnCheckPluginShould {

    @Test
    public void check_file_with_no_broken_links() {
        final String content = getFile("AggregateSampleFile");
        final Optional<InvalidFqnUsage> result = FqnCheckPlugin.check(content);
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void check_file_with_long_FQN_name() {
        final String content = getFile("PackageInfoSampleFile");
        final Optional<InvalidFqnUsage> result = FqnCheckPlugin.check(content);
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void check_file_with_no_javadoc() {
        final String content = getFile("NoJavadoc");
        final Optional<InvalidFqnUsage> result = FqnCheckPlugin.check(content);
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void check_file_with_corrupted_javadoc() {
        final String content = getFile("AggregateCorruptedSampleFile");
        final Optional<InvalidFqnUsage> result = FqnCheckPlugin.check(content);
        Assert.assertTrue(result.isPresent());
    }

    private String getFile(String fileName) {

        String result = "";

        ClassLoader classLoader = getClass().getClassLoader();
        try {
            final InputStream resourceAsStream = classLoader.getResourceAsStream(fileName);
            result = IOUtils.toString(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
