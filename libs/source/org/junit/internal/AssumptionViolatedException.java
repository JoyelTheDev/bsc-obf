/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.SelfDescribing;
import org.hamcrest.StringDescription;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class AssumptionViolatedException
extends RuntimeException
implements SelfDescribing {
    private static final long serialVersionUID = 2L;
    private final String fAssumption;
    private final boolean fValueMatcher;
    private final Object fValue;
    private final Matcher<?> fMatcher;

    @Deprecated
    public AssumptionViolatedException(String assumption, boolean hasValue, Object value, Matcher<?> matcher) {
        this.fAssumption = assumption;
        this.fValue = value;
        this.fMatcher = matcher;
        this.fValueMatcher = hasValue;
        if (value instanceof Throwable) {
            this.initCause((Throwable)value);
        }
    }

    @Deprecated
    public AssumptionViolatedException(Object value, Matcher<?> matcher) {
        this(null, true, value, matcher);
    }

    @Deprecated
    public AssumptionViolatedException(String assumption, Object value, Matcher<?> matcher) {
        this(assumption, true, value, matcher);
    }

    @Deprecated
    public AssumptionViolatedException(String assumption) {
        this(assumption, false, null, null);
    }

    @Deprecated
    public AssumptionViolatedException(String assumption, Throwable e) {
        this(assumption, false, null, null);
        this.initCause(e);
    }

    @Override
    public String getMessage() {
        return StringDescription.asString(this);
    }

    @Override
    public void describeTo(Description description) {
        if (this.fAssumption != null) {
            description.appendText(this.fAssumption);
        }
        if (this.fValueMatcher) {
            if (this.fAssumption != null) {
                description.appendText(": ");
            }
            description.appendText("got: ");
            description.appendValue(this.fValue);
            if (this.fMatcher != null) {
                description.appendText(", expected: ");
                description.appendDescriptionOf(this.fMatcher);
            }
        }
    }
}

