/*
 * Decompiled with CFR 0.152.
 */
package org.junit.rules;

import java.lang.management.ManagementFactory;
import java.util.List;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class DisableOnDebug
implements TestRule {
    private final TestRule rule;
    private final boolean debugging;

    public DisableOnDebug(TestRule rule) {
        this(rule, ManagementFactory.getRuntimeMXBean().getInputArguments());
    }

    DisableOnDebug(TestRule rule, List<String> inputArguments) {
        this.rule = rule;
        this.debugging = DisableOnDebug.isDebugging(inputArguments);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if (this.debugging) {
            return base;
        }
        return this.rule.apply(base, description);
    }

    private static boolean isDebugging(List<String> arguments) {
        for (String argument : arguments) {
            if ("-Xdebug".equals(argument)) {
                return true;
            }
            if (!argument.startsWith("-agentlib:jdwp")) continue;
            return true;
        }
        return false;
    }

    public boolean isDebugging() {
        return this.debugging;
    }
}

