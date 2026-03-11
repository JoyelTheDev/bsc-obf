/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.matchers;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ThrowableCauseMatcher<T extends Throwable>
extends TypeSafeMatcher<T> {
    private final Matcher<? extends Throwable> causeMatcher;

    public ThrowableCauseMatcher(Matcher<? extends Throwable> causeMatcher) {
        this.causeMatcher = causeMatcher;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("exception with cause ");
        description.appendDescriptionOf(this.causeMatcher);
    }

    @Override
    protected boolean matchesSafely(T item) {
        return this.causeMatcher.matches(((Throwable)item).getCause());
    }

    @Override
    protected void describeMismatchSafely(T item, Description description) {
        description.appendText("cause ");
        this.causeMatcher.describeMismatch(((Throwable)item).getCause(), description);
    }

    @Factory
    public static <T extends Throwable> Matcher<T> hasCause(Matcher<? extends Throwable> matcher) {
        return new ThrowableCauseMatcher<T>(matcher);
    }
}

