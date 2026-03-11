/*
 * Decompiled with CFR 0.152.
 */
package org.junit.rules;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public interface TestRule {
    public Statement apply(Statement var1, Description var2);
}

