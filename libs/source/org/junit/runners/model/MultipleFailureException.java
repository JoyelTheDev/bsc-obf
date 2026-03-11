/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.internal.Throwables;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class MultipleFailureException
extends Exception {
    private static final long serialVersionUID = 1L;
    private final List<Throwable> fErrors;

    public MultipleFailureException(List<Throwable> errors) {
        this.fErrors = new ArrayList<Throwable>(errors);
    }

    public List<Throwable> getFailures() {
        return Collections.unmodifiableList(this.fErrors);
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(String.format("There were %d errors:", this.fErrors.size()));
        for (Throwable e : this.fErrors) {
            sb.append(String.format("\n  %s(%s)", e.getClass().getName(), e.getMessage()));
        }
        return sb.toString();
    }

    public static void assertEmpty(List<Throwable> errors) throws Exception {
        if (errors.isEmpty()) {
            return;
        }
        if (errors.size() == 1) {
            throw Throwables.rethrowAsException(errors.get(0));
        }
        throw new org.junit.internal.runners.model.MultipleFailureException(errors);
    }
}

