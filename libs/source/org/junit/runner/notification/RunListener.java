/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runner.notification;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class RunListener {
    public void testRunStarted(Description description) throws Exception {
    }

    public void testRunFinished(Result result) throws Exception {
    }

    public void testStarted(Description description) throws Exception {
    }

    public void testFinished(Description description) throws Exception {
    }

    public void testFailure(Failure failure) throws Exception {
    }

    public void testAssumptionFailure(Failure failure) {
    }

    public void testIgnored(Description description) throws Exception {
    }

    @Documented
    @Target(value={ElementType.TYPE})
    @Retention(value=RetentionPolicy.RUNTIME)
    public static @interface ThreadSafe {
    }
}

