/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.runners.model;

import java.util.List;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
@Deprecated
public class MultipleFailureException
extends org.junit.runners.model.MultipleFailureException {
    private static final long serialVersionUID = 1L;

    public MultipleFailureException(List<Throwable> errors) {
        super(errors);
    }
}

