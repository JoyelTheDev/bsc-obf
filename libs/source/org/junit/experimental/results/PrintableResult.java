/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.results;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.junit.experimental.results.FailureList;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class PrintableResult {
    private Result result;

    public static PrintableResult testResult(Class<?> type) {
        return PrintableResult.testResult(Request.aClass(type));
    }

    public static PrintableResult testResult(Request request) {
        return new PrintableResult(new JUnitCore().run(request));
    }

    public PrintableResult(List<Failure> failures) {
        this(new FailureList(failures).result());
    }

    private PrintableResult(Result result) {
        this.result = result;
    }

    public int failureCount() {
        return this.result.getFailures().size();
    }

    public String toString() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new TextListener(new PrintStream(stream)).testRunFinished(this.result);
        return stream.toString();
    }
}

