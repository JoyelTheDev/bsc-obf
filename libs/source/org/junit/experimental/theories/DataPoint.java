/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.theories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value={ElementType.FIELD, ElementType.METHOD})
public @interface DataPoint {
    public String[] value() default {};

    public Class<? extends Throwable>[] ignoredExceptions() default {};
}

