/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.runners;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ErrorReportingRunner
extends Runner {
    private final List<Throwable> causes;
    private final Class<?> testClass;

    public ErrorReportingRunner(Class<?> testClass, Throwable cause) {
        if (testClass == null) {
            throw new NullPointerException("Test class cannot be null");
        }
        this.testClass = testClass;
        this.causes = this.getCauses(cause);
    }

    @Override
    public Description getDescription() {
        Description description = Description.createSuiteDescription(this.testClass);
        for (Throwable each : this.causes) {
            description.addChild(this.describeCause(each));
        }
        return description;
    }

    @Override
    public void run(RunNotifier notifier) {
        for (Throwable each : this.causes) {
            this.runCause(each, notifier);
        }
    }

    private List<Throwable> getCauses(Throwable cause) {
        if (cause instanceof InvocationTargetException) {
            return this.getCauses(cause.getCause());
        }
        if (cause instanceof org.junit.runners.model.InitializationError) {
            return ((org.junit.runners.model.InitializationError)cause).getCauses();
        }
        if (cause instanceof InitializationError) {
            return ((InitializationError)cause).getCauses();
        }
        return Arrays.asList(cause);
    }

    private Description describeCause(Throwable child) {
        return Description.createTestDescription(this.testClass, "initializationError");
    }

    private void runCause(Throwable child, RunNotifier notifier) {
        Description description = this.describeCause(child);
        notifier.fireTestStarted(description);
        notifier.fireTestFailure(new Failure(description, child));
        notifier.fireTestFinished(description);
    }
}

