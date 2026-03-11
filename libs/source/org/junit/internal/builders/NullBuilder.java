/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.builders;

import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class NullBuilder
extends RunnerBuilder {
    @Override
    public Runner runnerForClass(Class<?> each) throws Throwable {
        return null;
    }
}

