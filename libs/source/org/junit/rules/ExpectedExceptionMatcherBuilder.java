/*
 * Decompiled with CFR 0.152.
 */
package org.junit.rules;

import java.util.ArrayList;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.matchers.JUnitMatchers;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
class ExpectedExceptionMatcherBuilder {
    private final List<Matcher<?>> matchers = new ArrayList();

    ExpectedExceptionMatcherBuilder() {
    }

    void add(Matcher<?> matcher) {
        this.matchers.add(matcher);
    }

    boolean expectsThrowable() {
        return !this.matchers.isEmpty();
    }

    Matcher<Throwable> build() {
        return JUnitMatchers.isThrowable(this.allOfTheMatchers());
    }

    private Matcher<Throwable> allOfTheMatchers() {
        if (this.matchers.size() == 1) {
            return this.cast(this.matchers.get(0));
        }
        return CoreMatchers.allOf(this.castedMatchers());
    }

    private List<Matcher<? super Throwable>> castedMatchers() {
        return new ArrayList<Matcher<? super Throwable>>(this.matchers);
    }

    private Matcher<Throwable> cast(Matcher<?> singleMatcher) {
        return singleMatcher;
    }
}

