/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runner.notification;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

@RunListener.ThreadSafe
final class SynchronizedRunListener
extends RunListener {
    private final RunListener listener;
    private final Object monitor;

    SynchronizedRunListener(RunListener listener, Object monitor) {
        this.listener = listener;
        this.monitor = monitor;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void testRunStarted(Description description) throws Exception {
        Object object = this.monitor;
        synchronized (object) {
            this.listener.testRunStarted(description);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void testRunFinished(Result result) throws Exception {
        Object object = this.monitor;
        synchronized (object) {
            this.listener.testRunFinished(result);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void testStarted(Description description) throws Exception {
        Object object = this.monitor;
        synchronized (object) {
            this.listener.testStarted(description);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void testFinished(Description description) throws Exception {
        Object object = this.monitor;
        synchronized (object) {
            this.listener.testFinished(description);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void testFailure(Failure failure) throws Exception {
        Object object = this.monitor;
        synchronized (object) {
            this.listener.testFailure(failure);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void testAssumptionFailure(Failure failure) {
        Object object = this.monitor;
        synchronized (object) {
            this.listener.testAssumptionFailure(failure);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void testIgnored(Description description) throws Exception {
        Object object = this.monitor;
        synchronized (object) {
            this.listener.testIgnored(description);
        }
    }

    public int hashCode() {
        return this.listener.hashCode();
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SynchronizedRunListener)) {
            return false;
        }
        SynchronizedRunListener that = (SynchronizedRunListener)other;
        return this.listener.equals(that.listener);
    }

    public String toString() {
        return this.listener.toString() + " (with synchronization wrapper)";
    }
}

