/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal;

import java.io.PrintStream;
import org.junit.internal.JUnitSystem;

public class RealSystem
implements JUnitSystem {
    @Deprecated
    public void exit(int code) {
        System.exit(code);
    }

    public PrintStream out() {
        return System.out;
    }
}

