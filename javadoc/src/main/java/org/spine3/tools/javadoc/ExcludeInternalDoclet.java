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

package org.spine3.tools.javadoc;

import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Doc;
import com.sun.tools.javadoc.MethodDocImpl;
import com.sun.javadoc.RootDoc;
import com.sun.tools.doclets.standard.Standard;
import com.sun.tools.javadoc.Main;
import org.spine3.Internal;
import org.spine3.util.Exceptions;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Extension of {@linkplain Standard} doclet, which excludes {@linkplain Internal}-annotated components.
 *
 * <p>Use it to generate documentation for audience, that should not know about
 * {@linkplain Internal}-annotated components.
 *
 * <p>Works by pre-processing a {@linkplain RootDoc}.
 * The doclet creates new {@linkplain RootDoc},
 * that does not contain {@linkplain Internal}-annotated components and further generates documents.
 *
 * <p>You can use the non-standard doclet by specifying the following Javadoc options:
 * <ul>
 *     <li>doclet org.spine3.tools.javadoc.ExcludeInternalDoclet;</li>
 *     <li>docletpath classpathlist (The path to the doclet starting class file).</li>
 * </ul>
 *
 * <p>Call it with Javadoc tool like this:
 * <pre> {@code javadoc -doclet org.spine3.tools.javadoc.ExcludeInternalDoclet -docletpath "classpathlist" ...}</pre>
 *
 * <p>If everything done right, you will get the standard documentation generated by Javadoc tool,
 * with exception of {@linkplain Internal}-annotated components.
 *
 * @author Dmytro Grankin
 */
@SuppressWarnings("ExtendsUtilityClass")
public class ExcludeInternalDoclet extends Standard {

    private final ExcludePrinciple excludePrinciple;

    private ExcludeInternalDoclet(ExcludePrinciple excludePrinciple) {
        super();
        this.excludePrinciple = excludePrinciple;
    }

    /**
     * Entry point for the Javadoc tool.
     *
     * @param args the command-line parameters
     */
    public static void main(String[] args) {
        final String name = ExcludeInternalDoclet.class.getName();
        Main.execute(name, name, args);
    }

    /**
     * The "start" method as required by Javadoc.
     *
     * @param root the root of the documentation tree
     * @return {@code true} if the doclet ran without encountering any errors, {@code false} otherwise
     */
    @SuppressWarnings("unused") // called by com.sun.tools.javadoc.Main
    public static boolean start(RootDoc root) {
        final ExcludePrinciple excludePrinciple = new ExcludeInternalPrinciple(root);
        final ExcludeInternalDoclet doclet = new ExcludeInternalDoclet(excludePrinciple);
        return Standard.start((RootDoc) doclet.process(root, RootDoc.class));
    }

    /**
     * Creates proxy of "com.sun..." interfaces and excludes {@linkplain Doc}s
     * using {@linkplain #excludePrinciple}.
     *
     * @param returnValue the value to process
     * @param returnValueType the expected type of value
     * @return the processed value
     */
    @Nullable
    private Object process(@Nullable Object returnValue, Class returnValueType) {
        if (returnValue == null) {
            return null;
        }

        if (returnValue.getClass().getName().startsWith("com.sun.")) {
            final Class cls = returnValue.getClass();
            return Proxy.newProxyInstance(cls.getClassLoader(), cls.getInterfaces(), new ExcludeHandler(returnValue));
        } else if (returnValue instanceof Object[] && returnValueType.getComponentType() != null) {
            final Class componentType = returnValueType.getComponentType();
            final Object[] array = (Object[]) returnValue;
            final List<Object> list = new ArrayList<>();
            for (Object entry : array) {
                if (!(entry instanceof Doc && excludePrinciple.shouldExclude((Doc) entry))) {
                    list.add(process(entry, componentType));
                }
            }
            return list.toArray((Object[]) Array.newInstance(componentType, list.size()));
        } else {
            return returnValue;
        }
    }

    /**
     * Forms {@linkplain RootDoc} without {@linkplain Internal}-annotated elements.
     */
    private class ExcludeHandler implements InvocationHandler {

        private final Object target;

        private ExcludeHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args != null && IgnoredMethod.isIgnored(method.getName())) {
                args[0] = unwrap(args[0]);
            }
            try {
                return process(method.invoke(target, args), method.getReturnType());
            } catch (InvocationTargetException e) {
                throw Exceptions.wrappedCause(e);
            }
        }

        private Object unwrap(Object proxy) {
            if (proxy instanceof Proxy) {
                return ((ExcludeHandler) Proxy.getInvocationHandler(proxy)).target;
            }
            return proxy;
        }
    }

    /**
     * Enumeration of method names used in {@linkplain Standard} doclet implementation,
     * that cast parameter represented by interface to concrete implementation type.
     *
     * <p>For example {@linkplain MethodDocImpl#overrides(MethodDoc)}:
     * <pre> {@code
     *   public boolean overrides(MethodDoc meth) {
     *       MethodSymbol overridee = ((MethodDocImpl) meth).sym;
     *       // Remaining part omitted.
     *   }
     * }</pre>
     *
     * <p>In reason of we use proxy to filter Javadocs, we should unwrap proxy
     * of parameters passed to these methods to prevent {@code ClassCastException}.
     */
    @SuppressWarnings("unused") // Used in implicit form.
    private enum IgnoredMethod {
        COMPARE_TO("compareTo"),
        EQUALS("equals"),
        OVERRIDES("overrides"),
        SUBCLASS_OF("subclassOf");

        private final String methodName;

        IgnoredMethod(String methodName) {
            this.methodName = methodName;
        }

        String getMethodName() {
            return methodName;
        }

        /**
         * Returns {@code true} if the passed method name is one of {@linkplain IgnoredMethod}s.
         *
         * @param methodName the method name to test
         * @return {@code true} if the method name is one of {@linkplain IgnoredMethod}s
         */
        private static boolean isIgnored(String methodName) {
            for (IgnoredMethod ignoredMethod : IgnoredMethod.values()) {
                if (methodName.equals(ignoredMethod.getMethodName())) {
                    return true;
                }
            }

            return false;
        }
    }
}
