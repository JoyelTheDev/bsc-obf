/*
 * Decompiled with CFR 0.152.
 */
package org.junit.rules;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

@Deprecated
public class TestWatchman
implements MethodRule {
    public Statement apply(final Statement base, final FrameworkMethod method, Object target) {
        return new Statement(){

            public void evaluate() throws Throwable {
                TestWatchman.this.starting(method);
                try {
                    base.evaluate();
                    TestWatchman.this.succeeded(method);
                }
                catch (AssumptionViolatedException e) {
                    throw e;
                }
                catch (Throwable e) {
                    TestWatchman.this.failed(e, method);
                    throw e;
                }
                finally {
                    TestWatchman.this.finished(method);
                }
            }
        };
    }

    public void succeeded(FrameworkMethod method) {
    }

    public void failed(Throwable e, FrameworkMethod method) {
    }

    public void starting(FrameworkMethod method) {
    }

    public void finished(FrameworkMethod method) {
    }
}

