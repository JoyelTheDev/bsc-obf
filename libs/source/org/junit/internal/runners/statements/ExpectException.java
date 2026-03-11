/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.runners.statements;

import org.junit.internal.AssumptionViolatedException;
import org.junit.runners.model.Statement;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ExpectException
extends Statement {
    private final Statement next;
    private final Class<? extends Throwable> expected;

    public ExpectException(Statement next, Class<? extends Throwable> expected) {
        this.next = next;
        this.expected = expected;
    }

    @Override
    public void evaluate() throws Exception {
        boolean complete;
        block4: {
            complete = false;
            try {
                this.next.evaluate();
                complete = true;
            }
            catch (AssumptionViolatedException e) {
                throw e;
            }
            catch (Throwable e) {
                if (this.expected.isAssignableFrom(e.getClass())) break block4;
                String message = "Unexpected exception, expected<" + this.expected.getName() + "> but was<" + e.getClass().getName() + ">";
                throw new Exception(message, e);
            }
        }
        if (complete) {
            throw new AssertionError((Object)("Expected exception: " + this.expected.getName()));
        }
    }
}

