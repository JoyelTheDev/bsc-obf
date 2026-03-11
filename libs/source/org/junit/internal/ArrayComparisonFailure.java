/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal;

import java.util.ArrayList;
import java.util.List;

public class ArrayComparisonFailure
extends AssertionError {
    private static final long serialVersionUID = 1L;
    private final List<Integer> fIndices = new ArrayList<Integer>();
    private final String fMessage;

    public ArrayComparisonFailure(String message, AssertionError cause, int index) {
        this.fMessage = message;
        this.initCause((Throwable)((Object)cause));
        this.addDimension(index);
    }

    public void addDimension(int index) {
        this.fIndices.add(0, index);
    }

    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        if (this.fMessage != null) {
            sb.append(this.fMessage);
        }
        sb.append("arrays first differed at element ");
        for (int each : this.fIndices) {
            sb.append("[");
            sb.append(each);
            sb.append("]");
        }
        sb.append("; ");
        sb.append(this.getCause().getMessage());
        return sb.toString();
    }

    public String toString() {
        return this.getMessage();
    }
}

