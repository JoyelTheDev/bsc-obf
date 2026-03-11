/*
 * Decompiled with CFR 0.152.
 */
package org.junit;

import org.hamcrest.Matcher;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class AssumptionViolatedException
extends org.junit.internal.AssumptionViolatedException {
    private static final long serialVersionUID = 1L;

    public <T> AssumptionViolatedException(T actual, Matcher<T> matcher) {
        super(actual, matcher);
    }

    public <T> AssumptionViolatedException(String message, T expected, Matcher<T> matcher) {
        super(message, expected, matcher);
    }

    public AssumptionViolatedException(String message) {
        super(message);
    }

    public AssumptionViolatedException(String assumption, Throwable t) {
        super(assumption, t);
    }
}

