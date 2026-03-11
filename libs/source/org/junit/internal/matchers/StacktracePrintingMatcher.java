/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.matchers;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class StacktracePrintingMatcher<T extends Throwable>
extends TypeSafeMatcher<T> {
    private final Matcher<T> throwableMatcher;

    public StacktracePrintingMatcher(Matcher<T> throwableMatcher) {
        this.throwableMatcher = throwableMatcher;
    }

    @Override
    public void describeTo(Description description) {
        this.throwableMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(T item) {
        return this.throwableMatcher.matches(item);
    }

    @Override
    protected void describeMismatchSafely(T item, Description description) {
        this.throwableMatcher.describeMismatch(item, description);
        description.appendText("\nStacktrace was: ");
        description.appendText(this.readStacktrace((Throwable)item));
    }

    private String readStacktrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    @Factory
    public static <T extends Throwable> Matcher<T> isThrowable(Matcher<T> throwableMatcher) {
        return new StacktracePrintingMatcher<T>(throwableMatcher);
    }

    @Factory
    public static <T extends Exception> Matcher<T> isException(Matcher<T> exceptionMatcher) {
        return new StacktracePrintingMatcher<T>(exceptionMatcher);
    }
}

