/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal;

import java.lang.reflect.Array;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.internal.ArrayComparisonFailure;

public abstract class ComparisonCriteria {
    public void arrayEquals(String message, Object expecteds, Object actuals) throws ArrayComparisonFailure {
        if (expecteds == actuals || Arrays.deepEquals(new Object[]{expecteds}, new Object[]{actuals})) {
            return;
        }
        String header = message == null ? "" : message + ": ";
        int expectedsLength = this.assertArraysAreSameLength(expecteds, actuals, header);
        for (int i = 0; i < expectedsLength; ++i) {
            Object expected = Array.get(expecteds, i);
            Object actual = Array.get(actuals, i);
            if (this.isArray(expected) && this.isArray(actual)) {
                try {
                    this.arrayEquals(message, expected, actual);
                    continue;
                }
                catch (ArrayComparisonFailure e) {
                    e.addDimension(i);
                    throw e;
                }
            }
            try {
                this.assertElementsEqual(expected, actual);
                continue;
            }
            catch (AssertionError e) {
                throw new ArrayComparisonFailure(header, e, i);
            }
        }
    }

    private boolean isArray(Object expected) {
        return expected != null && expected.getClass().isArray();
    }

    private int assertArraysAreSameLength(Object expecteds, Object actuals, String header) {
        int expectedsLength;
        int actualsLength;
        if (expecteds == null) {
            Assert.fail(header + "expected array was null");
        }
        if (actuals == null) {
            Assert.fail(header + "actual array was null");
        }
        if ((actualsLength = Array.getLength(actuals)) != (expectedsLength = Array.getLength(expecteds))) {
            Assert.fail(header + "array lengths differed, expected.length=" + expectedsLength + " actual.length=" + actualsLength);
        }
        return expectedsLength;
    }

    protected abstract void assertElementsEqual(Object var1, Object var2);
}

