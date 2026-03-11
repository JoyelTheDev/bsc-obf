/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.categories;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.experimental.categories.CategoryValidator;
import org.junit.validator.ValidateWith;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
@Retention(value=RetentionPolicy.RUNTIME)
@Inherited
@ValidateWith(value=CategoryValidator.class)
public @interface Category {
    public Class<?>[] value();
}

