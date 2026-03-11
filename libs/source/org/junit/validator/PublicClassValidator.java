/*
 * Decompiled with CFR 0.152.
 */
package org.junit.validator;

import java.util.Collections;
import java.util.List;
import org.junit.runners.model.TestClass;
import org.junit.validator.TestClassValidator;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class PublicClassValidator
implements TestClassValidator {
    private static final List<Exception> NO_VALIDATION_ERRORS = Collections.emptyList();

    @Override
    public List<Exception> validateTestClass(TestClass testClass) {
        if (testClass.isPublic()) {
            return NO_VALIDATION_ERRORS;
        }
        return Collections.singletonList(new Exception("The class " + testClass.getName() + " is not public."));
    }
}

