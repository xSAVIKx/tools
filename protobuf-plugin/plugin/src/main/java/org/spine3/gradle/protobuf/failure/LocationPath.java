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

package org.spine3.gradle.protobuf.failure;

import com.google.protobuf.DescriptorProtos;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Encapsulates a {@linkplain DescriptorProtos.SourceCodeInfo.Location#getPathList() location path}.
 *
 * <p>A location path represents {@linkplain DescriptorProtos.SourceCodeInfo.Location#getPathList()
 * list of integers}, that used to identify a {@link DescriptorProtos.SourceCodeInfo.Location}
 * in a ".proto" file.
 *
 * @author Dmytro Grankin
 */
public class LocationPath {

    private final List<Integer> path;

    /**
     * Creates an empty location path.
     */
    public LocationPath() {
        this.path = new LinkedList<>();
    }

    /**
     * Creates a new instance.
     *
     * @param locationPath the list of path items
     */
    public LocationPath(List<Integer> locationPath) {
        this.path = locationPath;
    }

    /**
     * Appends the path item to the end of this path location.
     *
     * @param pathItem the path item
     */
    public void add(int pathItem) {
        path.add(pathItem);
    }

    /**
     * Appends the location path to the end of this path location.
     *
     * @param locationPath the location path
     */
    public void addAll(LocationPath locationPath) {
        path.addAll(locationPath.path);
    }

    public List<Integer> getPath() {
        return Collections.unmodifiableList(path);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LocationPath that = (LocationPath) o;

        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
