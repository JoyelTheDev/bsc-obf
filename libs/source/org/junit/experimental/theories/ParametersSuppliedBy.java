/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.theories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.experimental.theories.ParameterSupplier;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value={ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
public @interface ParametersSuppliedBy {
    public Class<? extends ParameterSupplier> value();
}

