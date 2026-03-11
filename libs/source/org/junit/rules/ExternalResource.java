/*
 * Decompiled with CFR 0.152.
 */
package org.junit.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public abstract class ExternalResource
implements TestRule {
    public Statement apply(Statement base, Description description) {
        return this.statement(base);
    }

    private Statement statement(final Statement base) {
        return new Statement(){

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            public void evaluate() throws Throwable {
                ExternalResource.this.before();
                try {
                    base.evaluate();
                }
                finally {
                    ExternalResource.this.after();
                }
            }
        };
    }

    protected void before() throws Throwable {
    }

    protected void after() {
    }
}

