/*
 * Decompiled with CFR 0.152.
 */
package org.junit.rules;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class RunRules
extends Statement {
    private final Statement statement;

    public RunRules(Statement base, Iterable<TestRule> rules, Description description) {
        this.statement = RunRules.applyAll(base, rules, description);
    }

    @Override
    public void evaluate() throws Throwable {
        this.statement.evaluate();
    }

    private static Statement applyAll(Statement result, Iterable<TestRule> rules, Description description) {
        for (TestRule each : rules) {
            result = each.apply(result, description);
        }
        return result;
    }
}

