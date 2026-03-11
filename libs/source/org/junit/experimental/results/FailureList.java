/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.results;

import java.util.List;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
class FailureList {
    private final List<Failure> failures;

    public FailureList(List<Failure> failures) {
        this.failures = failures;
    }

    public Result result() {
        Result result = new Result();
        RunListener listener = result.createListener();
        for (Failure failure : this.failures) {
            try {
                listener.testFailure(failure);
            }
            catch (Exception e) {
                throw new RuntimeException("I can't believe this happened");
            }
        }
        return result;
    }
}

