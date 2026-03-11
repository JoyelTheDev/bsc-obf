/*
 * Decompiled with CFR 0.152.
 */
package junit.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import junit.framework.AssertionFailedError;
import junit.framework.Protectable;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestListener;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class TestResult {
    protected List<TestFailure> fFailures = new ArrayList<TestFailure>();
    protected List<TestFailure> fErrors = new ArrayList<TestFailure>();
    protected List<TestListener> fListeners = new ArrayList<TestListener>();
    protected int fRunTests = 0;
    private boolean fStop = false;

    public synchronized void addError(Test test, Throwable e) {
        this.fErrors.add(new TestFailure(test, e));
        for (TestListener each : this.cloneListeners()) {
            each.addError(test, e);
        }
    }

    public synchronized void addFailure(Test test, AssertionFailedError e) {
        this.fFailures.add(new TestFailure(test, (Throwable)((Object)e)));
        for (TestListener each : this.cloneListeners()) {
            each.addFailure(test, e);
        }
    }

    public synchronized void addListener(TestListener listener) {
        this.fListeners.add(listener);
    }

    public synchronized void removeListener(TestListener listener) {
        this.fListeners.remove(listener);
    }

    private synchronized List<TestListener> cloneListeners() {
        ArrayList<TestListener> result = new ArrayList<TestListener>();
        result.addAll(this.fListeners);
        return result;
    }

    public void endTest(Test test) {
        for (TestListener each : this.cloneListeners()) {
            each.endTest(test);
        }
    }

    public synchronized int errorCount() {
        return this.fErrors.size();
    }

    public synchronized Enumeration<TestFailure> errors() {
        return Collections.enumeration(this.fErrors);
    }

    public synchronized int failureCount() {
        return this.fFailures.size();
    }

    public synchronized Enumeration<TestFailure> failures() {
        return Collections.enumeration(this.fFailures);
    }

    protected void run(final TestCase test) {
        this.startTest(test);
        Protectable p = new Protectable(){

            public void protect() throws Throwable {
                test.runBare();
            }
        };
        this.runProtected(test, p);
        this.endTest(test);
    }

    public synchronized int runCount() {
        return this.fRunTests;
    }

    public void runProtected(Test test, Protectable p) {
        try {
            p.protect();
        }
        catch (AssertionFailedError e) {
            this.addFailure(test, e);
        }
        catch (ThreadDeath e) {
            throw e;
        }
        catch (Throwable e) {
            this.addError(test, e);
        }
    }

    public synchronized boolean shouldStop() {
        return this.fStop;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void startTest(Test test) {
        int count = test.countTestCases();
        TestResult testResult = this;
        synchronized (testResult) {
            this.fRunTests += count;
        }
        for (TestListener each : this.cloneListeners()) {
            each.startTest(test);
        }
    }

    public synchronized void stop() {
        this.fStop = true;
    }

    public synchronized boolean wasSuccessful() {
        return this.failureCount() == 0 && this.errorCount() == 0;
    }
}

