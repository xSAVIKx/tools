package org.spine3.tools.javadoc;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.spine3.tools.javadoc.RootDocProxyReceiver.rootDocFor;

public class ExcludeInternalDocletShould {

    private static final String TEST_SOURCES_PACKAGE = "testsources";
    private static final String INTERNAL_PACKAGE = TEST_SOURCES_PACKAGE + ".internal";
    private static final String INTERNAL_METHOD_CLASS_FILENAME = "InternalMethodClass.java";
    private static final String INTERNAL_CLASS_FILENAME = "InternalClass.java";
    private static final String DERIVED_FROM_INTERNAL_CLASS_FILENAME = "DerivedFromInternalClass.java";

    @Test
    public void exclude_internal_annotated_annotations() {
        final String[] args = new CommandLineArgsBuilder()
                .addSource("InternalAnnotatedAnnotation.java")
                .build();

        final RootDoc rootDoc = rootDocFor(args);

        assertEquals(0, rootDoc.specifiedClasses().length);
    }

    @Test
    public void exclude_internal_ctors() {
        final String[] args = new CommandLineArgsBuilder()
                .addSource("InternalCtorClass.java")
                .build();

        final RootDoc rootDoc = rootDocFor(args);

        assertEquals(0, rootDoc.specifiedClasses()[0].constructors().length);
    }

    @Test
    public void exclude_internal_fields() {
        final String[] args = new CommandLineArgsBuilder()
                .addSource("InternalFieldClass.java")
                .build();

        final RootDoc rootDoc = rootDocFor(args);

        assertEquals(0, rootDoc.specifiedClasses()[0].fields().length);
    }

    @Test
    public void exclude_internal_methods() {
        final String[] args = new CommandLineArgsBuilder()
                .addSource(INTERNAL_METHOD_CLASS_FILENAME)
                .build();

        final RootDoc rootDoc = rootDocFor(args);

        assertEquals(0, rootDoc.specifiedClasses()[0].methods().length);
    }

    @Test
    public void exclude_internal_package_content() {
        final String[] args = new CommandLineArgsBuilder()
                .addSource("/internal/InternalPackageClass.java")
                .build();

        final RootDoc rootDoc = rootDocFor(args);

        assertEquals(0, rootDoc.specifiedClasses().length);
    }

    @Test
    public void exclude_only_from_internal_subpackages() {
        final String[] args = new CommandLineArgsBuilder()
                .addSource("/internal/subinternal/SubInternalPackageClass.java")
                .addSource("/notinternal/NotInternalClass.java")
                .addPackage(INTERNAL_PACKAGE)
                .addPackage(TEST_SOURCES_PACKAGE + ".notinternal")
                .build();

        final RootDoc rootDoc = rootDocFor(args);

        assertEquals(1, rootDoc.specifiedClasses().length);
    }

    @Test
    public void exclude_internal_classes() {
        final String[] args = new CommandLineArgsBuilder()
                .addSource(INTERNAL_CLASS_FILENAME)
                .build();

        final RootDoc rootDoc = rootDocFor(args);

        assertEquals(0, rootDoc.specifiedClasses().length);
    }

    @Test
    public void exclude_internal_interfaces() {
        final String[] args = new CommandLineArgsBuilder()
                .addSource("InternalAnnotatedInterface.java")
                .build();

        final RootDoc rootDoc = rootDocFor(args);

        assertEquals(0, rootDoc.specifiedClasses().length);
    }

    @Test
    public void exclude_internal_enums() {
        final String[] args = new CommandLineArgsBuilder()
                .addSource("InternalEnum.java")
                .build();

        final RootDoc rootDoc = rootDocFor(args);

        assertEquals(0, rootDoc.specifiedClasses().length);
    }

    @Test
    public void not_exclude_elements_annotated_with_Internal_located_in_another_package() {
        final String[] args = new CommandLineArgsBuilder()
                .addSource("GrpcInternalAnnotatedClass.java")
                .build();

        final RootDoc rootDoc = rootDocFor(args);

        assertEquals(1, rootDoc.specifiedClasses().length);
    }

    @Test
    public void correctly_work_when_compareTo_called() {
        final String[] args = new CommandLineArgsBuilder()
                .addSource(INTERNAL_CLASS_FILENAME)
                .addSource(DERIVED_FROM_INTERNAL_CLASS_FILENAME)
                .build();

        final RootDoc rootDoc = rootDocFor(args);

        // invoke compareTo to be sure, that proxy unwrapping
        // doest not expose object passed to compareTo method
        final ClassDoc anotherClassDoc = rootDoc.specifiedClasses()[0].superclass();
        rootDoc.specifiedClasses()[0].compareTo(anotherClassDoc);

        assertEquals(1, rootDoc.specifiedClasses().length);
    }

    @Test
    public void correctly_work_when_overrides_called() {
        final String[] args = new CommandLineArgsBuilder()
                .addSource(INTERNAL_METHOD_CLASS_FILENAME)
                .addSource("OverridesInternalMethod.java")
                .build();

        final RootDoc rootDoc = rootDocFor(args);

        // invoke overrides to be sure, that proxy unwrapping
        // doest not expose overridden method
        final ClassDoc overridesInternalMethod = rootDoc.classNamed(TEST_SOURCES_PACKAGE + ".OverridesInternalMethod");
        final MethodDoc overriddenMethod = overridesInternalMethod.methods()[0].overriddenMethod();
        overridesInternalMethod.methods()[0].overrides(overriddenMethod);

        assertEquals(0, rootDoc.classNamed(TEST_SOURCES_PACKAGE + ".InternalMethodClass").methods().length);
    }

    @Test
    public void correctly_work_when_subclassOf_invoked() {
        final String[] args = new CommandLineArgsBuilder()
                .addSource(INTERNAL_CLASS_FILENAME)
                .addSource(DERIVED_FROM_INTERNAL_CLASS_FILENAME)
                .build();

        final RootDoc rootDoc = rootDocFor(args);

        // invoke subclassOf to be sure, that proxy unwrapping
        // doest not expose parent internal class
        final ClassDoc superclass = rootDoc.specifiedClasses()[0].superclass();
        rootDoc.specifiedClasses()[0].subclassOf(superclass);

        assertEquals(1, rootDoc.specifiedClasses().length);
    }
}
