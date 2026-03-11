/*
 * Decompiled with CFR 0.152.
 */
package org.junit.rules;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.Assert;
import org.junit.internal.matchers.ThrowableCauseMatcher;
import org.junit.internal.matchers.ThrowableMessageMatcher;
import org.junit.rules.ExpectedExceptionMatcherBuilder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ExpectedException
implements TestRule {
    private final ExpectedExceptionMatcherBuilder matcherBuilder = new ExpectedExceptionMatcherBuilder();
    private String missingExceptionMessage = "Expected test to throw %s";

    public static ExpectedException none() {
        return new ExpectedException();
    }

    private ExpectedException() {
    }

    @Deprecated
    public ExpectedException handleAssertionErrors() {
        return this;
    }

    @Deprecated
    public ExpectedException handleAssumptionViolatedExceptions() {
        return this;
    }

    public ExpectedException reportMissingExceptionWithMessage(String message) {
        this.missingExceptionMessage = message;
        return this;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new ExpectedExceptionStatement(base);
    }

    public void expect(Matcher<?> matcher) {
        this.matcherBuilder.add(matcher);
    }

    public void expect(Class<? extends Throwable> type) {
        this.expect(CoreMatchers.instanceOf(type));
    }

    public void expectMessage(String substring) {
        this.expectMessage(CoreMatchers.containsString(substring));
    }

    public void expectMessage(Matcher<String> matcher) {
        this.expect(ThrowableMessageMatcher.hasMessage(matcher));
    }

    public void expectCause(Matcher<? extends Throwable> expectedCause) {
        this.expect(ThrowableCauseMatcher.hasCause(expectedCause));
    }

    private void handleException(Throwable e) throws Throwable {
        if (!this.isAnyExceptionExpected()) {
            throw e;
        }
        Assert.assertThat(e, this.matcherBuilder.build());
    }

    private boolean isAnyExceptionExpected() {
        return this.matcherBuilder.expectsThrowable();
    }

    private void failDueToMissingException() throws AssertionError {
        Assert.fail(this.missingExceptionMessage());
    }

    private String missingExceptionMessage() {
        String expectation = StringDescription.toString(this.matcherBuilder.build());
        return String.format(this.missingExceptionMessage, expectation);
    }

    private class ExpectedExceptionStatement
    extends Statement {
        private final Statement next;

        public ExpectedExceptionStatement(Statement base) {
            this.next = base;
        }

        public void evaluate() throws Throwable {
            try {
                this.next.evaluate();
            }
            catch (Throwable e) {
                ExpectedException.this.handleException(e);
                return;
            }
            if (ExpectedException.this.isAnyExceptionExpected()) {
                ExpectedException.this.failDueToMissingException();
            }
        }
    }
}

