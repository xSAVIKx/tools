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

package org.spine3.gradle.protobuf

import org.gradle.api.Project
import org.junit.Before
import org.junit.Test

import static org.spine3.gradle.protobuf.Given.*
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

@SuppressWarnings("GroovyInstanceMethodNamingConvention")
class ExtensionShould {

    private Project project

    @Before
    void setUp() {
        project = newProject()
        project.pluginManager.apply(SPINE_PROTOBUF_PLUGIN_ID)
    }

    @Test
    void return_default_mainTargetGenResourcesDir_if_not_set() {
        final GString dir = Extension.getMainTargetGenResourcesDir(project)

        assertNotEmptyAndIsInProjectDir(dir)
    }

    @Test
    void return_mainTargetGenResourcesDir_if_set() {
        project.spineProtobuf.mainTargetGenResourcesDir = newUuid()

        final GString dir = Extension.getMainTargetGenResourcesDir(project)

        assertEquals(project.spineProtobuf.mainTargetGenResourcesDir, dir)
    }

    @Test
    void return_default_testTargetGenResourcesDir_if_not_set() {
        final GString dir = Extension.getTestTargetGenResourcesDir(project)

        assertNotEmptyAndIsInProjectDir(dir)
    }

    @Test
    void return_testTargetGenResourcesDir_if_set() {
        project.spineProtobuf.testTargetGenResourcesDir = newUuid()

        final GString dir = Extension.getTestTargetGenResourcesDir(project)

        assertEquals(project.spineProtobuf.testTargetGenResourcesDir, dir)
    }

    @Test
    void return_default_mainDescriptorSetPath_if_not_set() {
        final GString dir = Extension.getMainDescriptorSetPath(project)

        assertNotEmptyAndIsInProjectDir(dir)
    }

    @Test
    void return_mainDescriptorSetPath_if_set() {
        project.spineProtobuf.mainDescriptorSetPath = newUuid()

        final GString dir = Extension.getMainDescriptorSetPath(project)

        assertEquals(project.spineProtobuf.mainDescriptorSetPath, dir)
    }

    @Test
    void return_default_testDescriptorSetPath_if_not_set() {
        final GString dir = Extension.getTestDescriptorSetPath(project)

        assertNotEmptyAndIsInProjectDir(dir)
    }

    @Test
    void return_testDescriptorSetPath_if_set() {
        project.spineProtobuf.testDescriptorSetPath = newUuid()

        final GString dir = Extension.getTestDescriptorSetPath(project)

        assertEquals(project.spineProtobuf.testDescriptorSetPath, dir)
    }

    @Test
    void return_default_targetGenFailuresRootDir_if_not_set() {
        final GString dir = Extension.getTargetGenFailuresRootDir(project)

        assertNotEmptyAndIsInProjectDir(dir)
    }

    @Test
    void return_targetGenFailuresRootDir_if_set() {
        project.spineProtobuf.targetGenFailuresRootDir = newUuid()

        final GString dir = Extension.getTargetGenFailuresRootDir(project)

        assertEquals(project.spineProtobuf.targetGenFailuresRootDir, dir)
    }

    @Test
    void return_default_dirsToClean_if_not_set() {
        final List<GString> actualDirs = Extension.getDirsToClean(project)

        assertEquals(1, actualDirs.size())
        assertNotEmptyAndIsInProjectDir(actualDirs.get(0))
    }

    @Test
    void return_single_dirToClean_if_set() {
        project.spineProtobuf.dirToClean = newUuid()

        final List<GString> actualDirs = Extension.getDirsToClean(project)

        assertEquals(1, actualDirs.size())
        assertEquals(project.spineProtobuf.dirToClean, actualDirs.get(0))
    }

    @Test
    void return_dirsToClean_list_if_array_is_set() {
        project.spineProtobuf.dirsToClean = [newUuid(), newUuid()]

        final List<GString> actualDirs = Extension.getDirsToClean(project)

        assertEquals(project.spineProtobuf.dirsToClean, actualDirs.toArray())
    }

    @Test
    void return_dirsToClean_list_if_array_and_single_are_set() {
        project.spineProtobuf.dirsToClean = [newUuid(), newUuid()]
        project.spineProtobuf.dirToClean = newUuid()

        final List<GString> actualDirs = Extension.getDirsToClean(project)

        assertEquals(project.spineProtobuf.dirsToClean, actualDirs.toArray())
    }

    private void assertNotEmptyAndIsInProjectDir(GString path) {
        assertFalse(path.trim().isEmpty())
        assertTrue(path.startsWith("$project.projectDir.absolutePath"))
    }
}
