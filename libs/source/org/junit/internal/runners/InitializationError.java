/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.runners;

import java.util.Arrays;
import java.util.List;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
@Deprecated
public class InitializationError
extends Exception {
    private static final long serialVersionUID = 1L;
    private final List<Throwable> fErrors;

    public InitializationError(List<Throwable> errors) {
        this.fErrors = errors;
    }

    public InitializationError(Throwable ... errors) {
        this(Arrays.asList(errors));
    }

    public InitializationError(String string) {
        this(new Throwable[]{new Exception(string)});
    }

    public List<Throwable> getCauses() {
        return this.fErrors;
    }
}

