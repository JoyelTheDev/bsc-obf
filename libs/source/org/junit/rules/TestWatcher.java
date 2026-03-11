/*
 * Decompiled with CFR 0.152.
 */
package org.junit.rules;

import java.util.ArrayList;
import java.util.List;
import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public abstract class TestWatcher
implements TestRule {
    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            public void evaluate() throws Throwable {
                ArrayList<Throwable> errors = new ArrayList<Throwable>();
                TestWatcher.this.startingQuietly(description, errors);
                try {
                    base.evaluate();
                    TestWatcher.this.succeededQuietly(description, errors);
                }
                catch (org.junit.internal.AssumptionViolatedException e) {
                    errors.add(e);
                    TestWatcher.this.skippedQuietly(e, description, errors);
                }
                catch (Throwable e) {
                    errors.add(e);
                    TestWatcher.this.failedQuietly(e, description, errors);
                }
                finally {
                    TestWatcher.this.finishedQuietly(description, errors);
                }
                MultipleFailureException.assertEmpty(errors);
            }
        };
    }

    private void succeededQuietly(Description description, List<Throwable> errors) {
        try {
            this.succeeded(description);
        }
        catch (Throwable e) {
            errors.add(e);
        }
    }

    private void failedQuietly(Throwable e, Description description, List<Throwable> errors) {
        try {
            this.failed(e, description);
        }
        catch (Throwable e1) {
            errors.add(e1);
        }
    }

    private void skippedQuietly(org.junit.internal.AssumptionViolatedException e, Description description, List<Throwable> errors) {
        try {
            if (e instanceof AssumptionViolatedException) {
                this.skipped((AssumptionViolatedException)e, description);
            } else {
                this.skipped(e, description);
            }
        }
        catch (Throwable e1) {
            errors.add(e1);
        }
    }

    private void startingQuietly(Description description, List<Throwable> errors) {
        try {
            this.starting(description);
        }
        catch (Throwable e) {
            errors.add(e);
        }
    }

    private void finishedQuietly(Description description, List<Throwable> errors) {
        try {
            this.finished(description);
        }
        catch (Throwable e) {
            errors.add(e);
        }
    }

    protected void succeeded(Description description) {
    }

    protected void failed(Throwable e, Description description) {
    }

    protected void skipped(AssumptionViolatedException e, Description description) {
        AssumptionViolatedException asInternalException = e;
        this.skipped((org.junit.internal.AssumptionViolatedException)asInternalException, description);
    }

    @Deprecated
    protected void skipped(org.junit.internal.AssumptionViolatedException e, Description description) {
    }

    protected void starting(Description description) {
    }

    protected void finished(Description description) {
    }
}

