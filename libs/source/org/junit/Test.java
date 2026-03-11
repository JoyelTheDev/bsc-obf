/*
 * Decompiled with CFR 0.152.
 */
package org.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value={ElementType.METHOD})
public @interface Test {
    public Class<? extends Throwable> expected() default None.class;

    public long timeout() default 0L;

    public static class None
    extends Throwable {
        private static final long serialVersionUID = 1L;

        private None() {
        }
    }
}

