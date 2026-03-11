/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.builders;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class IgnoredClassRunner
extends Runner {
    private final Class<?> clazz;

    public IgnoredClassRunner(Class<?> testClass) {
        this.clazz = testClass;
    }

    @Override
    public void run(RunNotifier notifier) {
        notifier.fireTestIgnored(this.getDescription());
    }

    @Override
    public Description getDescription() {
        return Description.createSuiteDescription(this.clazz);
    }
}

