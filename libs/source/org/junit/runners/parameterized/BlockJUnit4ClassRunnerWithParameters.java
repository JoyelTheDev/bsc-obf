/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners.parameterized;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.parameterized.TestWithParameters;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class BlockJUnit4ClassRunnerWithParameters
extends BlockJUnit4ClassRunner {
    private final Object[] parameters;
    private final String name;

    public BlockJUnit4ClassRunnerWithParameters(TestWithParameters test) throws InitializationError {
        super(test.getTestClass().getJavaClass());
        this.parameters = test.getParameters().toArray(new Object[test.getParameters().size()]);
        this.name = test.getName();
    }

    @Override
    public Object createTest() throws Exception {
        if (this.fieldsAreAnnotated()) {
            return this.createTestUsingFieldInjection();
        }
        return this.createTestUsingConstructorInjection();
    }

    private Object createTestUsingConstructorInjection() throws Exception {
        return this.getTestClass().getOnlyConstructor().newInstance(this.parameters);
    }

    private Object createTestUsingFieldInjection() throws Exception {
        List<FrameworkField> annotatedFieldsByParameter = this.getAnnotatedFieldsByParameter();
        if (annotatedFieldsByParameter.size() != this.parameters.length) {
            throw new Exception("Wrong number of parameters and @Parameter fields. @Parameter fields counted: " + annotatedFieldsByParameter.size() + ", available parameters: " + this.parameters.length + ".");
        }
        Object testClassInstance = this.getTestClass().getJavaClass().newInstance();
        for (FrameworkField each : annotatedFieldsByParameter) {
            Field field = each.getField();
            Parameterized.Parameter annotation = field.getAnnotation(Parameterized.Parameter.class);
            int index = annotation.value();
            try {
                field.set(testClassInstance, this.parameters[index]);
            }
            catch (IllegalArgumentException iare) {
                throw new Exception(this.getTestClass().getName() + ": Trying to set " + field.getName() + " with the value " + this.parameters[index] + " that is not the right type (" + this.parameters[index].getClass().getSimpleName() + " instead of " + field.getType().getSimpleName() + ").", iare);
            }
        }
        return testClassInstance;
    }

    @Override
    protected String getName() {
        return this.name;
    }

    @Override
    protected String testName(FrameworkMethod method) {
        return method.getName() + this.getName();
    }

    @Override
    protected void validateConstructor(List<Throwable> errors) {
        this.validateOnlyOneConstructor(errors);
        if (this.fieldsAreAnnotated()) {
            this.validateZeroArgConstructor(errors);
        }
    }

    @Override
    protected void validateFields(List<Throwable> errors) {
        super.validateFields(errors);
        if (this.fieldsAreAnnotated()) {
            List<FrameworkField> annotatedFieldsByParameter = this.getAnnotatedFieldsByParameter();
            int[] usedIndices = new int[annotatedFieldsByParameter.size()];
            for (FrameworkField each : annotatedFieldsByParameter) {
                int index = each.getField().getAnnotation(Parameterized.Parameter.class).value();
                if (index < 0 || index > annotatedFieldsByParameter.size() - 1) {
                    errors.add(new Exception("Invalid @Parameter value: " + index + ". @Parameter fields counted: " + annotatedFieldsByParameter.size() + ". Please use an index between 0 and " + (annotatedFieldsByParameter.size() - 1) + "."));
                    continue;
                }
                int n = index;
                usedIndices[n] = usedIndices[n] + 1;
            }
            for (int index = 0; index < usedIndices.length; ++index) {
                int numberOfUse = usedIndices[index];
                if (numberOfUse == 0) {
                    errors.add(new Exception("@Parameter(" + index + ") is never used."));
                    continue;
                }
                if (numberOfUse <= 1) continue;
                errors.add(new Exception("@Parameter(" + index + ") is used more than once (" + numberOfUse + ")."));
            }
        }
    }

    @Override
    protected Statement classBlock(RunNotifier notifier) {
        return this.childrenInvoker(notifier);
    }

    @Override
    protected Annotation[] getRunnerAnnotations() {
        return new Annotation[0];
    }

    private List<FrameworkField> getAnnotatedFieldsByParameter() {
        return this.getTestClass().getAnnotatedFields(Parameterized.Parameter.class);
    }

    private boolean fieldsAreAnnotated() {
        return !this.getAnnotatedFieldsByParameter().isEmpty();
    }
}

