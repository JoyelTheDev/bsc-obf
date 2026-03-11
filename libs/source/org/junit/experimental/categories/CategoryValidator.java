/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.categories;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runners.model.FrameworkMethod;
import org.junit.validator.AnnotationValidator;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class CategoryValidator
extends AnnotationValidator {
    private static final Set<Class<? extends Annotation>> INCOMPATIBLE_ANNOTATIONS = Collections.unmodifiableSet(new HashSet<Class>(Arrays.asList(BeforeClass.class, AfterClass.class, Before.class, After.class)));

    @Override
    public List<Exception> validateAnnotatedMethod(FrameworkMethod method) {
        Annotation[] annotations;
        ArrayList<Exception> errors = new ArrayList<Exception>();
        for (Annotation annotation : annotations = method.getAnnotations()) {
            for (Class<? extends Annotation> clazz : INCOMPATIBLE_ANNOTATIONS) {
                if (!annotation.annotationType().isAssignableFrom(clazz)) continue;
                this.addErrorMessage(errors, clazz);
            }
        }
        return Collections.unmodifiableList(errors);
    }

    private void addErrorMessage(List<Exception> errors, Class<?> clazz) {
        String message = String.format("@%s can not be combined with @Category", clazz.getSimpleName());
        errors.add(new Exception(message));
    }
}

