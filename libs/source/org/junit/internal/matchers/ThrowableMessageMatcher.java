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
public class ThrowableMessageMatcher<T extends Throwable>
extends TypeSafeMatcher<T> {
    private final Matcher<String> matcher;

    public ThrowableMessageMatcher(Matcher<String> matcher) {
        this.matcher = matcher;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("exception with message ");
        description.appendDescriptionOf(this.matcher);
    }

    @Override
    protected boolean matchesSafely(T item) {
        return this.matcher.matches(((Throwable)item).getMessage());
    }

    @Override
    protected void describeMismatchSafely(T item, Description description) {
        description.appendText("message ");
        this.matcher.describeMismatch(((Throwable)item).getMessage(), description);
    }

    @Factory
    public static <T extends Throwable> Matcher<T> hasMessage(Matcher<String> matcher) {
        return new ThrowableMessageMatcher<T>(matcher);
    }
}

