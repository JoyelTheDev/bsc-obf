/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.builders;

import java.lang.reflect.Modifier;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class AnnotatedBuilder
extends RunnerBuilder {
    private static final String CONSTRUCTOR_ERROR_FORMAT = "Custom runner class %s should have a public constructor with signature %s(Class testClass)";
    private final RunnerBuilder suiteBuilder;

    public AnnotatedBuilder(RunnerBuilder suiteBuilder) {
        this.suiteBuilder = suiteBuilder;
    }

    @Override
    public Runner runnerForClass(Class<?> testClass) throws Exception {
        Class<?> currentTestClass = testClass;
        while (currentTestClass != null) {
            RunWith annotation = currentTestClass.getAnnotation(RunWith.class);
            if (annotation != null) {
                return this.buildRunner(annotation.value(), testClass);
            }
            currentTestClass = this.getEnclosingClassForNonStaticMemberClass(currentTestClass);
        }
        return null;
    }

    private Class<?> getEnclosingClassForNonStaticMemberClass(Class<?> currentTestClass) {
        if (currentTestClass.isMemberClass() && !Modifier.isStatic(currentTestClass.getModifiers())) {
            return currentTestClass.getEnclosingClass();
        }
        return null;
    }

    public Runner buildRunner(Class<? extends Runner> runnerClass, Class<?> testClass) throws Exception {
        try {
            return runnerClass.getConstructor(Class.class).newInstance(testClass);
        }
        catch (NoSuchMethodException e) {
            try {
                return runnerClass.getConstructor(Class.class, RunnerBuilder.class).newInstance(testClass, this.suiteBuilder);
            }
            catch (NoSuchMethodException e2) {
                String simpleName = runnerClass.getSimpleName();
                throw new InitializationError(String.format(CONSTRUCTOR_ERROR_FORMAT, simpleName, simpleName));
            }
        }
    }
}

