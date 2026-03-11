/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.runners;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.MethodSorter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
@Deprecated
public class TestClass {
    private final Class<?> klass;

    public TestClass(Class<?> klass) {
        this.klass = klass;
    }

    public List<Method> getTestMethods() {
        return this.getAnnotatedMethods(Test.class);
    }

    List<Method> getBefores() {
        return this.getAnnotatedMethods(BeforeClass.class);
    }

    List<Method> getAfters() {
        return this.getAnnotatedMethods(AfterClass.class);
    }

    public List<Method> getAnnotatedMethods(Class<? extends Annotation> annotationClass) {
        ArrayList<Method> results = new ArrayList<Method>();
        for (Class<?> eachClass : this.getSuperClasses(this.klass)) {
            Method[] methods;
            for (Method eachMethod : methods = MethodSorter.getDeclaredMethods(eachClass)) {
                Annotation annotation = eachMethod.getAnnotation(annotationClass);
                if (annotation == null || this.isShadowed(eachMethod, results)) continue;
                results.add(eachMethod);
            }
        }
        if (this.runsTopToBottom(annotationClass)) {
            Collections.reverse(results);
        }
        return results;
    }

    private boolean runsTopToBottom(Class<? extends Annotation> annotation) {
        return annotation.equals(Before.class) || annotation.equals(BeforeClass.class);
    }

    private boolean isShadowed(Method method, List<Method> results) {
        for (Method each : results) {
            if (!this.isShadowed(method, each)) continue;
            return true;
        }
        return false;
    }

    private boolean isShadowed(Method current, Method previous) {
        if (!previous.getName().equals(current.getName())) {
            return false;
        }
        if (previous.getParameterTypes().length != current.getParameterTypes().length) {
            return false;
        }
        for (int i = 0; i < previous.getParameterTypes().length; ++i) {
            if (previous.getParameterTypes()[i].equals(current.getParameterTypes()[i])) continue;
            return false;
        }
        return true;
    }

    private List<Class<?>> getSuperClasses(Class<?> testClass) {
        ArrayList results = new ArrayList();
        for (Class<?> current = testClass; current != null; current = current.getSuperclass()) {
            results.add(current);
        }
        return results;
    }

    public Constructor<?> getConstructor() throws SecurityException, NoSuchMethodException {
        return this.klass.getConstructor(new Class[0]);
    }

    public Class<?> getJavaClass() {
        return this.klass;
    }

    public String getName() {
        return this.klass.getName();
    }
}

