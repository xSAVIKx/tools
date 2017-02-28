package org.spine3.tools.javadoc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class CommandLineArgsBuilder {

    private static final String ABSOLUTE_APP_PATH = System.getProperty("user.dir");
    private static final String TEST_SOURCES_DIR = ABSOLUTE_APP_PATH + "/src/test/resources/testsources/";
    private static final String RESOURCES_DIR = ABSOLUTE_APP_PATH + "/src/test/resources";

    private final Collection<String> classes = new ArrayList<>();
    private final Collection<String> packages = new ArrayList<>();

    CommandLineArgsBuilder addSource(String sourceName) {
        classes.add(TEST_SOURCES_DIR + sourceName);
        return this;
    }

    CommandLineArgsBuilder addPackage(String packageName) {
        packages.add(packageName);
        return this;
    }

    String[] build() {
        final List<String> allArguments = new ArrayList<>();

        addSourcePath(allArguments);
        allArguments.addAll(packages);
        allArguments.addAll(classes);

        return allArguments.toArray(new String[allArguments.size()]);
    }

    private static void addSourcePath(Collection<String> commandLineArgs) {
        // Path to scan packages
        commandLineArgs.add("-sourcepath");
        commandLineArgs.add(RESOURCES_DIR);
    }
}
