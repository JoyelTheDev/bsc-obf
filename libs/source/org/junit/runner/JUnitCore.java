/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runner;

import junit.framework.Test;
import junit.runner.Version;
import org.junit.internal.JUnitSystem;
import org.junit.internal.RealSystem;
import org.junit.internal.TextListener;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCommandLineParseResult;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class JUnitCore {
    private final RunNotifier notifier = new RunNotifier();

    public static void main(String ... args) {
        Result result = new JUnitCore().runMain(new RealSystem(), args);
        System.exit(result.wasSuccessful() ? 0 : 1);
    }

    public static Result runClasses(Class<?> ... classes) {
        return JUnitCore.runClasses(JUnitCore.defaultComputer(), classes);
    }

    public static Result runClasses(Computer computer, Class<?> ... classes) {
        return new JUnitCore().run(computer, classes);
    }

    Result runMain(JUnitSystem system, String ... args) {
        system.out().println("JUnit version " + Version.id());
        JUnitCommandLineParseResult jUnitCommandLineParseResult = JUnitCommandLineParseResult.parse(args);
        TextListener listener = new TextListener(system);
        this.addListener(listener);
        return this.run(jUnitCommandLineParseResult.createRequest(JUnitCore.defaultComputer()));
    }

    public String getVersion() {
        return Version.id();
    }

    public Result run(Class<?> ... classes) {
        return this.run(JUnitCore.defaultComputer(), classes);
    }

    public Result run(Computer computer, Class<?> ... classes) {
        return this.run(Request.classes(computer, classes));
    }

    public Result run(Request request) {
        return this.run(request.getRunner());
    }

    public Result run(Test test) {
        return this.run(new JUnit38ClassRunner(test));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Result run(Runner runner) {
        Result result = new Result();
        RunListener listener = result.createListener();
        this.notifier.addFirstListener(listener);
        try {
            this.notifier.fireTestRunStarted(runner.getDescription());
            runner.run(this.notifier);
            this.notifier.fireTestRunFinished(result);
        }
        finally {
            this.removeListener(listener);
        }
        return result;
    }

    public void addListener(RunListener listener) {
        this.notifier.addListener(listener);
    }

    public void removeListener(RunListener listener) {
        this.notifier.removeListener(listener);
    }

    static Computer defaultComputer() {
        return new Computer();
    }
}

