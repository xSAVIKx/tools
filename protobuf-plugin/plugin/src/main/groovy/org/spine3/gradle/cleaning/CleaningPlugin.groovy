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

package org.spine3.gradle.cleaning

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer

import static org.spine3.gradle.Extension.getDirsToClean

/**
 * Plugin which performs additional cleaning on clean task.
 */
class CleaningPlugin implements Plugin<Project> {

    /**
     * Adds `:preClean` task, which is executed before `:clean` task.
     */
    @Override
    void apply(Project project) {
        final Task preClean = project.task("preClean") << {
            deleteDirs(getDirsToClean(project))
        }
        final TaskContainer tasks = project.getTasks()
        tasks.getByPath("clean").dependsOn(preClean)
    }

    private static void deleteDirs(List<String> dirs) {
        for (String dirPath : dirs) {
            final File file = new File(dirPath)
            if (file.exists() && file.isDirectory()) {
                file.deleteDir()
            }
        }
    }
}
