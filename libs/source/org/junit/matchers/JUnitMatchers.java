/*
 * Decompiled with CFR 0.152.
 */
package org.junit.matchers;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.core.CombinableMatcher;
import org.junit.internal.matchers.StacktracePrintingMatcher;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class JUnitMatchers {
    @Deprecated
    public static <T> Matcher<Iterable<? super T>> hasItem(T element) {
        return CoreMatchers.hasItem(element);
    }

    @Deprecated
    public static <T> Matcher<Iterable<? super T>> hasItem(Matcher<? super T> elementMatcher) {
        return CoreMatchers.hasItem(elementMatcher);
    }

    @Deprecated
    public static <T> Matcher<Iterable<T>> hasItems(T ... elements) {
        return CoreMatchers.hasItems(elements);
    }

    @Deprecated
    public static <T> Matcher<Iterable<T>> hasItems(Matcher<? super T> ... elementMatchers) {
        return CoreMatchers.hasItems(elementMatchers);
    }

    @Deprecated
    public static <T> Matcher<Iterable<T>> everyItem(Matcher<T> elementMatcher) {
        return CoreMatchers.everyItem(elementMatcher);
    }

    @Deprecated
    public static Matcher<String> containsString(String substring) {
        return CoreMatchers.containsString(substring);
    }

    @Deprecated
    public static <T> CombinableMatcher.CombinableBothMatcher<T> both(Matcher<? super T> matcher) {
        return CoreMatchers.both(matcher);
    }

    @Deprecated
    public static <T> CombinableMatcher.CombinableEitherMatcher<T> either(Matcher<? super T> matcher) {
        return CoreMatchers.either(matcher);
    }

    public static <T extends Throwable> Matcher<T> isThrowable(Matcher<T> throwableMatcher) {
        return StacktracePrintingMatcher.isThrowable(throwableMatcher);
    }

    public static <T extends Exception> Matcher<T> isException(Matcher<T> exceptionMatcher) {
        return StacktracePrintingMatcher.isException(exceptionMatcher);
    }
}

