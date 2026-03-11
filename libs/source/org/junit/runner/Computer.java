/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runner;

import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Computer {
    public static Computer serial() {
        return new Computer();
    }

    public Runner getSuite(final RunnerBuilder builder, Class<?>[] classes) throws InitializationError {
        return new Suite(new RunnerBuilder(){

            @Override
            public Runner runnerForClass(Class<?> testClass) throws Throwable {
                return Computer.this.getRunner(builder, testClass);
            }
        }, classes);
    }

    protected Runner getRunner(RunnerBuilder builder, Class<?> testClass) throws Throwable {
        return builder.runnerForClass(testClass);
    }
}

