/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.builders;

import org.junit.Ignore;
import org.junit.internal.builders.IgnoredClassRunner;
import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class IgnoredBuilder
extends RunnerBuilder {
    @Override
    public Runner runnerForClass(Class<?> testClass) {
        if (testClass.getAnnotation(Ignore.class) != null) {
            return new IgnoredClassRunner(testClass);
        }
        return null;
    }
}

