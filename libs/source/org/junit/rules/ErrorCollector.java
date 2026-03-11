/*
 * Decompiled with CFR 0.152.
 */
package org.junit.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.rules.Verifier;
import org.junit.runners.model.MultipleFailureException;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ErrorCollector
extends Verifier {
    private List<Throwable> errors = new ArrayList<Throwable>();

    @Override
    protected void verify() throws Throwable {
        MultipleFailureException.assertEmpty(this.errors);
    }

    public void addError(Throwable error) {
        this.errors.add(error);
    }

    public <T> void checkThat(T value, Matcher<T> matcher) {
        this.checkThat("", value, matcher);
    }

    public <T> void checkThat(final String reason, final T value, final Matcher<T> matcher) {
        this.checkSucceeds(new Callable<Object>(){

            @Override
            public Object call() throws Exception {
                Assert.assertThat(reason, value, matcher);
                return value;
            }
        });
    }

    public <T> T checkSucceeds(Callable<T> callable) {
        try {
            return callable.call();
        }
        catch (Throwable e) {
            this.addError(e);
            return null;
        }
    }
}

