/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class Throwables {
    private Throwables() {
    }

    public static Exception rethrowAsException(Throwable e) throws Exception {
        Throwables.rethrow(e);
        return null;
    }

    private static <T extends Throwable> void rethrow(Throwable e) throws T {
        throw e;
    }
}

