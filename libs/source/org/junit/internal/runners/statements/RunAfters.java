/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.runners.statements;

import java.util.ArrayList;
import java.util.List;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class RunAfters
extends Statement {
    private final Statement next;
    private final Object target;
    private final List<FrameworkMethod> afters;

    public RunAfters(Statement next, List<FrameworkMethod> afters, Object target) {
        this.next = next;
        this.afters = afters;
        this.target = target;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void evaluate() throws Throwable {
        ArrayList<Throwable> errors = new ArrayList<Throwable>();
        try {
            this.next.evaluate();
        }
        catch (Throwable e) {
            errors.add(e);
        }
        finally {
            for (FrameworkMethod each : this.afters) {
                try {
                    each.invokeExplosively(this.target, new Object[0]);
                }
                catch (Throwable e) {
                    errors.add(e);
                }
            }
        }
        MultipleFailureException.assertEmpty(errors);
    }
}

