/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runner;

import org.junit.runner.FilterFactoryParams;
import org.junit.runner.manipulation.Filter;

public interface FilterFactory {
    public Filter createFilter(FilterFactoryParams var1) throws FilterNotCreatedException;

    public static class FilterNotCreatedException
    extends Exception {
        public FilterNotCreatedException(Exception exception) {
            super(exception.getMessage(), exception);
        }
    }
}

