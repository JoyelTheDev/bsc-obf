/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.matchers;

import java.lang.reflect.Method;
import org.hamcrest.BaseMatcher;
import org.junit.internal.MethodSorter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
@Deprecated
public abstract class TypeSafeMatcher<T>
extends BaseMatcher<T> {
    private Class<?> expectedType;

    public abstract boolean matchesSafely(T var1);

    protected TypeSafeMatcher() {
        this.expectedType = TypeSafeMatcher.findExpectedType(this.getClass());
    }

    private static Class<?> findExpectedType(Class<?> fromClass) {
        for (Class<?> c = fromClass; c != Object.class; c = c.getSuperclass()) {
            for (Method method : MethodSorter.getDeclaredMethods(c)) {
                if (!TypeSafeMatcher.isMatchesSafelyMethod(method)) continue;
                return method.getParameterTypes()[0];
            }
        }
        throw new Error("Cannot determine correct type for matchesSafely() method.");
    }

    private static boolean isMatchesSafelyMethod(Method method) {
        return method.getName().equals("matchesSafely") && method.getParameterTypes().length == 1 && !method.isSynthetic();
    }

    protected TypeSafeMatcher(Class<T> expectedType) {
        this.expectedType = expectedType;
    }

    @Override
    public final boolean matches(Object item) {
        return item != null && this.expectedType.isInstance(item) && this.matchesSafely(item);
    }
}

