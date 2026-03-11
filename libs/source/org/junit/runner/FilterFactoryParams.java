/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runner;

import org.junit.runner.Description;

public final class FilterFactoryParams {
    private final Description topLevelDescription;
    private final String args;

    public FilterFactoryParams(Description topLevelDescription, String args) {
        if (args == null || topLevelDescription == null) {
            throw new NullPointerException();
        }
        this.topLevelDescription = topLevelDescription;
        this.args = args;
    }

    public String getArgs() {
        return this.args;
    }

    public Description getTopLevelDescription() {
        return this.topLevelDescription;
    }
}

