/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.runners;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import org.junit.runners.Suite;
import org.junit.runners.model.RunnerBuilder;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Enclosed
extends Suite {
    public Enclosed(Class<?> klass, RunnerBuilder builder) throws Throwable {
        super(builder, klass, Enclosed.filterAbstractClasses(klass.getClasses()));
    }

    private static Class<?>[] filterAbstractClasses(Class<?>[] classes) {
        ArrayList filteredList = new ArrayList(classes.length);
        for (Class<?> clazz : classes) {
            if (Modifier.isAbstract(clazz.getModifiers())) continue;
            filteredList.add(clazz);
        }
        return filteredList.toArray(new Class[filteredList.size()]);
    }
}

