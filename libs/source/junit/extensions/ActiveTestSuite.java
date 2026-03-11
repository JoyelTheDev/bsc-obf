/*
 * Decompiled with CFR 0.152.
 */
package junit.extensions;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ActiveTestSuite
extends TestSuite {
    private volatile int fActiveTestDeathCount;

    public ActiveTestSuite() {
    }

    public ActiveTestSuite(Class<? extends TestCase> theClass) {
        super((Class<?>)theClass);
    }

    public ActiveTestSuite(String name) {
        super(name);
    }

    public ActiveTestSuite(Class<? extends TestCase> theClass, String name) {
        super(theClass, name);
    }

    @Override
    public void run(TestResult result) {
        this.fActiveTestDeathCount = 0;
        super.run(result);
        this.waitUntilFinished();
    }

    @Override
    public void runTest(final Test test, final TestResult result) {
        Thread t = new Thread(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            public void run() {
                try {
                    test.run(result);
                }
                finally {
                    ActiveTestSuite.this.runFinished();
                }
            }
        };
        t.start();
    }

    synchronized void waitUntilFinished() {
        while (this.fActiveTestDeathCount < this.testCount()) {
            try {
                this.wait();
            }
            catch (InterruptedException e) {
                return;
            }
        }
    }

    public synchronized void runFinished() {
        ++this.fActiveTestDeathCount;
        this.notifyAll();
    }
}

