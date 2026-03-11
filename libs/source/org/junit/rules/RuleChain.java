/*
 * Decompiled with CFR 0.152.
 */
package org.junit.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class RuleChain
implements TestRule {
    private static final RuleChain EMPTY_CHAIN = new RuleChain(Collections.<TestRule>emptyList());
    private List<TestRule> rulesStartingWithInnerMost;

    public static RuleChain emptyRuleChain() {
        return EMPTY_CHAIN;
    }

    public static RuleChain outerRule(TestRule outerRule) {
        return RuleChain.emptyRuleChain().around(outerRule);
    }

    private RuleChain(List<TestRule> rules) {
        this.rulesStartingWithInnerMost = rules;
    }

    public RuleChain around(TestRule enclosedRule) {
        ArrayList<TestRule> rulesOfNewChain = new ArrayList<TestRule>();
        rulesOfNewChain.add(enclosedRule);
        rulesOfNewChain.addAll(this.rulesStartingWithInnerMost);
        return new RuleChain(rulesOfNewChain);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        for (TestRule each : this.rulesStartingWithInnerMost) {
            base = each.apply(base, description);
        }
        return base;
    }
}

