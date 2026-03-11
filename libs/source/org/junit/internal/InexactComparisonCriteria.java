/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal;

import org.junit.Assert;
import org.junit.internal.ComparisonCriteria;

public class InexactComparisonCriteria
extends ComparisonCriteria {
    public Object fDelta;

    public InexactComparisonCriteria(double delta) {
        this.fDelta = delta;
    }

    public InexactComparisonCriteria(float delta) {
        this.fDelta = Float.valueOf(delta);
    }

    protected void assertElementsEqual(Object expected, Object actual) {
        if (expected instanceof Double) {
            Assert.assertEquals((Double)expected, (double)((Double)actual), (double)((Double)this.fDelta));
        } else {
            Assert.assertEquals(((Float)expected).floatValue(), ((Float)actual).floatValue(), ((Float)this.fDelta).floatValue());
        }
    }
}

