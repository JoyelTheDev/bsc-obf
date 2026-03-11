/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners;

import java.lang.reflect.Method;
import java.util.Comparator;
import org.junit.internal.MethodSorter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public enum MethodSorters {
    NAME_ASCENDING(MethodSorter.NAME_ASCENDING),
    JVM(null),
    DEFAULT(MethodSorter.DEFAULT);

    private final Comparator<Method> comparator;

    private MethodSorters(Comparator<Method> comparator) {
        this.comparator = comparator;
    }

    public Comparator<Method> getComparator() {
        return this.comparator;
    }
}

