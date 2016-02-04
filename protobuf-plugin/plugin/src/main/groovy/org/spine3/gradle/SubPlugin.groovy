package org.spine3.gradle

import org.gradle.api.Project
import org.spine3.gradle.shared.SharedPreferences

/**
 * Describes the logical part of a plugin.
 */
interface SubPlugin {

    /**
     * The way of using sub-plugins.
     *
     * @param target target Project
     * @param sharedPreferences single stateful object for all plugins
     */
    void apply(Project target, SharedPreferences sharedPreferences);
}