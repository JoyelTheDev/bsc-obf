/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.builders;

import junit.framework.TestCase;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class JUnit3Builder
extends RunnerBuilder {
    @Override
    public Runner runnerForClass(Class<?> testClass) throws Throwable {
        if (this.isPre4Test(testClass)) {
            return new JUnit38ClassRunner(testClass);
        }
        return null;
    }

    boolean isPre4Test(Class<?> testClass) {
        return TestCase.class.isAssignableFrom(testClass);
    }
}

