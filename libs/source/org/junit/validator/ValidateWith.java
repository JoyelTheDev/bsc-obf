/*
 * Decompiled with CFR 0.152.
 */
package org.junit.validator;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.validator.AnnotationValidator;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
@Retention(value=RetentionPolicy.RUNTIME)
@Inherited
public @interface ValidateWith {
    public Class<? extends AnnotationValidator> value();
}

