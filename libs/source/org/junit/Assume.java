/*
 * Decompiled with CFR 0.152.
 */
package org.junit;

import java.util.Arrays;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.AssumptionViolatedException;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Assume {
    public static void assumeTrue(boolean b) {
        Assume.assumeThat(b, CoreMatchers.is(true));
    }

    public static void assumeFalse(boolean b) {
        Assume.assumeTrue(!b);
    }

    public static void assumeTrue(String message, boolean b) {
        if (!b) {
            throw new AssumptionViolatedException(message);
        }
    }

    public static void assumeFalse(String message, boolean b) {
        Assume.assumeTrue(message, !b);
    }

    public static void assumeNotNull(Object ... objects) {
        Assume.assumeThat(Arrays.asList(objects), CoreMatchers.everyItem(CoreMatchers.notNullValue()));
    }

    public static <T> void assumeThat(T actual, Matcher<T> matcher) {
        if (!matcher.matches(actual)) {
            throw new AssumptionViolatedException(actual, matcher);
        }
    }

    public static <T> void assumeThat(String message, T actual, Matcher<T> matcher) {
        if (!matcher.matches(actual)) {
            throw new AssumptionViolatedException(message, actual, matcher);
        }
    }

    public static void assumeNoException(Throwable e) {
        Assume.assumeThat(e, CoreMatchers.nullValue());
    }

    public static void assumeNoException(String message, Throwable e) {
        Assume.assumeThat(message, e, CoreMatchers.nullValue());
    }
}

