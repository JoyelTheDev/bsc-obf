/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Classes {
    public static Class<?> getClass(String className) throws ClassNotFoundException {
        return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
    }
}

