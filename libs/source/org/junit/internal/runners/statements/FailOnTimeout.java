/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.runners.statements;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestTimedOutException;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class FailOnTimeout
extends Statement {
    private final Statement originalStatement;
    private final TimeUnit timeUnit;
    private final long timeout;
    private final boolean lookForStuckThread;
    private volatile ThreadGroup threadGroup = null;

    public static Builder builder() {
        return new Builder();
    }

    @Deprecated
    public FailOnTimeout(Statement statement, long timeoutMillis) {
        this(FailOnTimeout.builder().withTimeout(timeoutMillis, TimeUnit.MILLISECONDS), statement);
    }

    private FailOnTimeout(Builder builder, Statement statement) {
        this.originalStatement = statement;
        this.timeout = builder.timeout;
        this.timeUnit = builder.unit;
        this.lookForStuckThread = builder.lookForStuckThread;
    }

    @Override
    public void evaluate() throws Throwable {
        CallableStatement callable = new CallableStatement();
        FutureTask<Throwable> task = new FutureTask<Throwable>(callable);
        this.threadGroup = new ThreadGroup("FailOnTimeoutGroup");
        Thread thread = new Thread(this.threadGroup, task, "Time-limited test");
        thread.setDaemon(true);
        thread.start();
        callable.awaitStarted();
        Throwable throwable = this.getResult(task, thread);
        if (throwable != null) {
            throw throwable;
        }
    }

    private Throwable getResult(FutureTask<Throwable> task, Thread thread) {
        try {
            if (this.timeout > 0L) {
                return task.get(this.timeout, this.timeUnit);
            }
            return task.get();
        }
        catch (InterruptedException e) {
            return e;
        }
        catch (ExecutionException e) {
            return e.getCause();
        }
        catch (TimeoutException e) {
            return this.createTimeoutException(thread);
        }
    }

    private Exception createTimeoutException(Thread thread) {
        StackTraceElement[] stackTrace = thread.getStackTrace();
        Thread stuckThread = this.lookForStuckThread ? this.getStuckThread(thread) : null;
        TestTimedOutException currThreadException = new TestTimedOutException(this.timeout, this.timeUnit);
        if (stackTrace != null) {
            currThreadException.setStackTrace(stackTrace);
            thread.interrupt();
        }
        if (stuckThread != null) {
            Exception stuckThreadException = new Exception("Appears to be stuck in thread " + stuckThread.getName());
            stuckThreadException.setStackTrace(this.getStackTrace(stuckThread));
            return new MultipleFailureException(Arrays.asList(currThreadException, stuckThreadException));
        }
        return currThreadException;
    }

    private StackTraceElement[] getStackTrace(Thread thread) {
        try {
            return thread.getStackTrace();
        }
        catch (SecurityException e) {
            return new StackTraceElement[0];
        }
    }

    private Thread getStuckThread(Thread mainThread) {
        if (this.threadGroup == null) {
            return null;
        }
        Thread[] threadsInGroup = this.getThreadArray(this.threadGroup);
        if (threadsInGroup == null) {
            return null;
        }
        Thread stuckThread = null;
        long maxCpuTime = 0L;
        for (Thread thread : threadsInGroup) {
            if (thread.getState() != Thread.State.RUNNABLE) continue;
            long threadCpuTime = this.cpuTime(thread);
            if (stuckThread != null && threadCpuTime <= maxCpuTime) continue;
            stuckThread = thread;
            maxCpuTime = threadCpuTime;
        }
        return stuckThread == mainThread ? null : stuckThread;
    }

    private Thread[] getThreadArray(ThreadGroup group) {
        Thread[] threads;
        int enumCount;
        int count = group.activeCount();
        int loopCount = 0;
        for (int enumSize = Math.max(count * 2, 100); (enumCount = group.enumerate(threads = new Thread[enumSize])) >= enumSize; enumSize += 100) {
            if (++loopCount < 5) continue;
            return null;
        }
        return this.copyThreads(threads, enumCount);
    }

    private Thread[] copyThreads(Thread[] threads, int count) {
        int length = Math.min(count, threads.length);
        Thread[] result = new Thread[length];
        for (int i = 0; i < length; ++i) {
            result[i] = threads[i];
        }
        return result;
    }

    private long cpuTime(Thread thr) {
        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
        if (mxBean.isThreadCpuTimeSupported()) {
            try {
                return mxBean.getThreadCpuTime(thr.getId());
            }
            catch (UnsupportedOperationException unsupportedOperationException) {
                // empty catch block
            }
        }
        return 0L;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private class CallableStatement
    implements Callable<Throwable> {
        private final CountDownLatch startLatch = new CountDownLatch(1);

        private CallableStatement() {
        }

        @Override
        public Throwable call() throws Exception {
            try {
                this.startLatch.countDown();
                FailOnTimeout.this.originalStatement.evaluate();
            }
            catch (Exception e) {
                throw e;
            }
            catch (Throwable e) {
                return e;
            }
            return null;
        }

        public void awaitStarted() throws InterruptedException {
            this.startLatch.await();
        }
    }

    public static class Builder {
        private boolean lookForStuckThread = false;
        private long timeout = 0L;
        private TimeUnit unit = TimeUnit.SECONDS;

        private Builder() {
        }

        public Builder withTimeout(long timeout, TimeUnit unit) {
            if (timeout < 0L) {
                throw new IllegalArgumentException("timeout must be non-negative");
            }
            if (unit == null) {
                throw new NullPointerException("TimeUnit cannot be null");
            }
            this.timeout = timeout;
            this.unit = unit;
            return this;
        }

        public Builder withLookingForStuckThread(boolean enable) {
            this.lookForStuckThread = enable;
            return this;
        }

        public FailOnTimeout build(Statement statement) {
            if (statement == null) {
                throw new NullPointerException("statement cannot be null");
            }
            return new FailOnTimeout(this, statement);
        }
    }
}

