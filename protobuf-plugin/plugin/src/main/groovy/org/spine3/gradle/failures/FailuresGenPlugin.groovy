package org.spine3.gradle.failures

import com.google.protobuf.Descriptors
import com.google.protobuf.Message
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@Slf4j
class FailuresGenPlugin implements Plugin<Project> {

    private static final String PACKAGE_PREFIX = "package ";
    private static final String METHOD_GET_DESCRIPTOR = "getDescriptor";
    private static final String FAILURE_PREFIX = "* Protobuf type {@code ";

    private String projectPath;
    private String javaClassesPath;

    @Override
    void apply(Project target) {
        projectPath = target.projectDir.absolutePath;

        final Task generateFailures = target.task("generateFailures") << {
            File failuresClassFile = seekForFailuresClassFile(target);
            if (failuresClassFile == null) {
                return;
            }
            FailuresFileMetadata failuresFileMetadata = readFailuresPackage(target);
            Class failuresClass = loadFailuresIntoClassPath(javaClassesPath, failuresFileMetadata);

            Descriptors.GenericDescriptor descriptor = getClassDescriptor(failuresClass);

            Descriptors.FileDescriptor fileDescriptor = descriptor.file;

            if (validateFailures(fileDescriptor)) {
                generateFailures(fileDescriptor, failuresFileMetadata.failuresPackage);
            } else {
                log.error("Invalid failures file");
            }
        };

        generateFailures.dependsOn("compileJava", "generateProto");
        target.getTasks().getByPath("processResources").dependsOn(generateFailures);
    }

    private File seekForFailuresClassFile(Project target) {
        javaClassesPath = "$projectPath/build/classes/main";
        String failuresClassName = "Failures.class";

        return seekForFile(target, javaClassesPath, failuresClassName);
    }

    private FailuresFileMetadata readFailuresPackage(Project target) {
        String javaInputPath = "$projectPath/generated/main/java";
        String failuresJavaName = "Failures.java";

        File failuresInputJava = seekForFile(target, javaInputPath, failuresJavaName);
        if (failuresInputJava == null || !failuresInputJava.exists()) {
            log.error("No failures found.");
        }
        List<String> lines = failuresInputJava.readLines();

        String javaPackage = null;
        String firstFoundFailure = null;

        for (String line : lines) {
            def trimmedLine = line.trim();
            def trimmedLineLength = trimmedLine.length();

            if (javaPackage == null && trimmedLine.startsWith(PACKAGE_PREFIX)) {
                javaPackage = trimmedLine.substring(PACKAGE_PREFIX.length(), trimmedLineLength - 1);
            } else if (firstFoundFailure == null && trimmedLine.startsWith(FAILURE_PREFIX)) {
                firstFoundFailure = trimmedLine.substring(trimmedLine.lastIndexOf('.') + 1, trimmedLineLength - 1);
            } else if (javaPackage != null && firstFoundFailure != null) {
                break;
            }
        }

        return new FailuresFileMetadata(javaPackage, firstFoundFailure);
    }

    private static boolean validateFailures(Descriptors.FileDescriptor descriptor) {
        return !(descriptor.options.javaMultipleFiles || (descriptor.options.javaOuterClassname != null
                && !descriptor.options.javaOuterClassname.isEmpty()
                && !descriptor.options.javaOuterClassname.equals("Failures")));
    }

    private void generateFailures(Descriptors.FileDescriptor descriptor, String javaPackage) {
        String failuresFolderPath = projectPath + "/generated/main/spine/" + javaPackage.replace(".", "/");

        Map<String, String> dependencyPackages = new HashMap<>();

        for (Descriptors.FileDescriptor dependency : descriptor.dependencies) {
            def dependencyFileName = dependency.file.fullName;
            def dependencyJavaPackage = dependency.options.javaPackage;
            dependencyPackages.put(dependencyFileName, dependencyJavaPackage);
        }

        final List<Descriptors.Descriptor> failures = descriptor.messageTypes;
        for (Descriptors.Descriptor failure : failures) {
            File outputFile = new File(failuresFolderPath + "/" + failure.name + ".java");
            writeFailureIntoFile(failure, outputFile, javaPackage, dependencyPackages);
        }

    }

    private static Class loadFailuresIntoClassPath(String javaClassesPath, FailuresFileMetadata metadata) {

        File javaClassesRoot = new File(javaClassesPath);

        URL fileUrl = javaClassesRoot.toURI().toURL();

        URL[] args = [fileUrl] as URL[];

        URLClassLoader classLoader = new URLClassLoader(args, this.classLoader);

        String className = metadata.failuresPackage + ".Failures\$" + metadata.sampleFailureName;

        Class failuresClass = classLoader.loadClass(className);

        return failuresClass;
    }

    private static File seekForFile(Project target, String rootPath, String fileName) {
        File result = null;

        File root = new File(rootPath);
        target.fileTree(root).each {
            if (it.name.equals(fileName)) {
                result = it;
            }
        }

        return result;
    }

    private static Descriptors.GenericDescriptor getClassDescriptor(Class clazz) {
        if (!Message.class.isAssignableFrom(clazz)) {
            log.error("Class " + clazz + " is not an instance of Protobuf Message");
            return null;
        }
        try {
            final Method method = clazz.getMethod(METHOD_GET_DESCRIPTOR);
            final Descriptors.GenericDescriptor result = (Descriptors.GenericDescriptor) method.invoke(null);
            return result;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            log.error("Could not get descriptor for type " + clazz.getName());
            return null;
        }
    }

    private static void writeFailureIntoFile(Descriptors.Descriptor failure, File file, String javaPackage,
                                             Map<String, String> dependencyPackages) {
        new FailureWriter(failure, file, javaPackage, dependencyPackages).write();
    }

    private class FailuresFileMetadata {
        public final String failuresPackage;
        public final String sampleFailureName;

        public FailuresFileMetadata(String failuresPackage, String sampleFailureName) {
            this.failuresPackage = failuresPackage;
            this.sampleFailureName = sampleFailureName;
        }
    }
}
