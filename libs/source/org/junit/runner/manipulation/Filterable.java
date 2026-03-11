/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runner.manipulation;

import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;

public interface Filterable {
    public void filter(Filter var1) throws NoTestsRemainException;
}

