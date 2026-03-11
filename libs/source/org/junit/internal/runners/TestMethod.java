/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.runners;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.internal.runners.TestClass;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
@Deprecated
public class TestMethod {
    private final Method method;
    private TestClass testClass;

    public TestMethod(Method method, TestClass testClass) {
        this.method = method;
        this.testClass = testClass;
    }

    public boolean isIgnored() {
        return this.method.getAnnotation(Ignore.class) != null;
    }

    public long getTimeout() {
        Test annotation = this.method.getAnnotation(Test.class);
        if (annotation == null) {
            return 0L;
        }
        long timeout = annotation.timeout();
        return timeout;
    }

    protected Class<? extends Throwable> getExpectedException() {
        Test annotation = this.method.getAnnotation(Test.class);
        if (annotation == null || annotation.expected() == Test.None.class) {
            return null;
        }
        return annotation.expected();
    }

    boolean isUnexpected(Throwable exception) {
        return !this.getExpectedException().isAssignableFrom(exception.getClass());
    }

    boolean expectsException() {
        return this.getExpectedException() != null;
    }

    List<Method> getBefores() {
        return this.testClass.getAnnotatedMethods(Before.class);
    }

    List<Method> getAfters() {
        return this.testClass.getAnnotatedMethods(After.class);
    }

    public void invoke(Object test) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        this.method.invoke(test, new Object[0]);
    }
}

