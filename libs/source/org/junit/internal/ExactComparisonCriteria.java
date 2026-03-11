/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal;

import org.junit.Assert;
import org.junit.internal.ComparisonCriteria;

public class ExactComparisonCriteria
extends ComparisonCriteria {
    protected void assertElementsEqual(Object expected, Object actual) {
        Assert.assertEquals(expected, actual);
    }
}

