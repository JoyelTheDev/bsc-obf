/*
 * Decompiled with CFR 0.152.
 */
package org.junit.validator;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.runners.model.Annotatable;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.junit.validator.AnnotationValidator;
import org.junit.validator.AnnotationValidatorFactory;
import org.junit.validator.TestClassValidator;
import org.junit.validator.ValidateWith;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class AnnotationsValidator
implements TestClassValidator {
    private static final List<AnnotatableValidator<?>> VALIDATORS = Arrays.asList(new ClassValidator(), new MethodValidator(), new FieldValidator());

    @Override
    public List<Exception> validateTestClass(TestClass testClass) {
        ArrayList<Exception> validationErrors = new ArrayList<Exception>();
        for (AnnotatableValidator<?> validator : VALIDATORS) {
            List<Exception> additionalErrors = validator.validateTestClass(testClass);
            validationErrors.addAll(additionalErrors);
        }
        return validationErrors;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class FieldValidator
    extends AnnotatableValidator<FrameworkField> {
        private FieldValidator() {
        }

        @Override
        Iterable<FrameworkField> getAnnotatablesForTestClass(TestClass testClass) {
            return testClass.getAnnotatedFields();
        }

        @Override
        List<Exception> validateAnnotatable(AnnotationValidator validator, FrameworkField field) {
            return validator.validateAnnotatedField(field);
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class MethodValidator
    extends AnnotatableValidator<FrameworkMethod> {
        private MethodValidator() {
        }

        @Override
        Iterable<FrameworkMethod> getAnnotatablesForTestClass(TestClass testClass) {
            return testClass.getAnnotatedMethods();
        }

        @Override
        List<Exception> validateAnnotatable(AnnotationValidator validator, FrameworkMethod method) {
            return validator.validateAnnotatedMethod(method);
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class ClassValidator
    extends AnnotatableValidator<TestClass> {
        private ClassValidator() {
        }

        @Override
        Iterable<TestClass> getAnnotatablesForTestClass(TestClass testClass) {
            return Collections.singletonList(testClass);
        }

        @Override
        List<Exception> validateAnnotatable(AnnotationValidator validator, TestClass testClass) {
            return validator.validateAnnotatedClass(testClass);
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static abstract class AnnotatableValidator<T extends Annotatable> {
        private static final AnnotationValidatorFactory ANNOTATION_VALIDATOR_FACTORY = new AnnotationValidatorFactory();

        private AnnotatableValidator() {
        }

        abstract Iterable<T> getAnnotatablesForTestClass(TestClass var1);

        abstract List<Exception> validateAnnotatable(AnnotationValidator var1, T var2);

        public List<Exception> validateTestClass(TestClass testClass) {
            ArrayList<Exception> validationErrors = new ArrayList<Exception>();
            for (Annotatable annotatable : this.getAnnotatablesForTestClass(testClass)) {
                List<Exception> additionalErrors = this.validateAnnotatable(annotatable);
                validationErrors.addAll(additionalErrors);
            }
            return validationErrors;
        }

        private List<Exception> validateAnnotatable(T annotatable) {
            ArrayList<Exception> validationErrors = new ArrayList<Exception>();
            for (Annotation annotation : annotatable.getAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                ValidateWith validateWith = annotationType.getAnnotation(ValidateWith.class);
                if (validateWith == null) continue;
                AnnotationValidator annotationValidator = ANNOTATION_VALIDATOR_FACTORY.createAnnotationValidator(validateWith);
                List<Exception> errors = this.validateAnnotatable(annotationValidator, annotatable);
                validationErrors.addAll(errors);
            }
            return validationErrors;
        }
    }
}

