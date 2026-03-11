/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.requests;

import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.runner.Request;
import org.junit.runner.Runner;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ClassRequest
extends Request {
    private final Object runnerLock = new Object();
    private final Class<?> fTestClass;
    private final boolean canUseSuiteMethod;
    private volatile Runner runner;

    public ClassRequest(Class<?> testClass, boolean canUseSuiteMethod) {
        this.fTestClass = testClass;
        this.canUseSuiteMethod = canUseSuiteMethod;
    }

    public ClassRequest(Class<?> testClass) {
        this(testClass, true);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Runner getRunner() {
        if (this.runner == null) {
            Object object = this.runnerLock;
            synchronized (object) {
                if (this.runner == null) {
                    this.runner = new AllDefaultPossibilitiesBuilder(this.canUseSuiteMethod).safeRunnerForClass(this.fTestClass);
                }
            }
        }
        return this.runner;
    }
}

