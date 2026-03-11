/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.theories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.experimental.theories.ParametersSuppliedBy;
import org.junit.experimental.theories.internal.SpecificDataPointsSupplier;

@Retention(value=RetentionPolicy.RUNTIME)
@Target(value={ElementType.PARAMETER})
@ParametersSuppliedBy(value=SpecificDataPointsSupplier.class)
public @interface FromDataPoints {
    public String value();
}

