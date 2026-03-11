/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.internal.runners.ErrorReportingRunner;
import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public abstract class RunnerBuilder {
    private final Set<Class<?>> parents = new HashSet();

    public abstract Runner runnerForClass(Class<?> var1) throws Throwable;

    public Runner safeRunnerForClass(Class<?> testClass) {
        try {
            return this.runnerForClass(testClass);
        }
        catch (Throwable e) {
            return new ErrorReportingRunner(testClass, e);
        }
    }

    Class<?> addParent(Class<?> parent) throws InitializationError {
        if (!this.parents.add(parent)) {
            throw new InitializationError(String.format("class '%s' (possibly indirectly) contains itself as a SuiteClass", parent.getName()));
        }
        return parent;
    }

    void removeParent(Class<?> klass) {
        this.parents.remove(klass);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<Runner> runners(Class<?> parent, Class<?>[] children) throws InitializationError {
        this.addParent(parent);
        try {
            List<Runner> list = this.runners(children);
            return list;
        }
        finally {
            this.removeParent(parent);
        }
    }

    public List<Runner> runners(Class<?> parent, List<Class<?>> children) throws InitializationError {
        return this.runners(parent, children.toArray(new Class[0]));
    }

    private List<Runner> runners(Class<?>[] children) {
        ArrayList<Runner> runners = new ArrayList<Runner>();
        for (Class<?> each : children) {
            Runner childRunner = this.safeRunnerForClass(each);
            if (childRunner == null) continue;
            runners.add(childRunner);
        }
        return runners;
    }
}

