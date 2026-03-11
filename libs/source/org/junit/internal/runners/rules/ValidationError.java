/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.runners.rules;

import java.lang.annotation.Annotation;
import org.junit.runners.model.FrameworkMember;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
class ValidationError
extends Exception {
    public ValidationError(FrameworkMember<?> member, Class<? extends Annotation> annotation, String suffix) {
        super(String.format("The @%s '%s' %s", annotation.getSimpleName(), member.getName(), suffix));
    }
}

