/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.runners;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.junit.internal.runners.ClassRoadie;
import org.junit.internal.runners.InitializationError;
import org.junit.internal.runners.MethodRoadie;
import org.junit.internal.runners.MethodValidator;
import org.junit.internal.runners.TestClass;
import org.junit.internal.runners.TestMethod;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
@Deprecated
public class JUnit4ClassRunner
extends Runner
implements Filterable,
Sortable {
    private final List<Method> testMethods;
    private TestClass testClass;

    public JUnit4ClassRunner(Class<?> klass) throws InitializationError {
        this.testClass = new TestClass(klass);
        this.testMethods = this.getTestMethods();
        this.validate();
    }

    protected List<Method> getTestMethods() {
        return this.testClass.getTestMethods();
    }

    protected void validate() throws InitializationError {
        MethodValidator methodValidator = new MethodValidator(this.testClass);
        methodValidator.validateMethodsForDefaultRunner();
        methodValidator.assertValid();
    }

    @Override
    public void run(final RunNotifier notifier) {
        new ClassRoadie(notifier, this.testClass, this.getDescription(), new Runnable(){

            public void run() {
                JUnit4ClassRunner.this.runMethods(notifier);
            }
        }).runProtected();
    }

    protected void runMethods(RunNotifier notifier) {
        for (Method method : this.testMethods) {
            this.invokeTestMethod(method, notifier);
        }
    }

    @Override
    public Description getDescription() {
        Description spec = Description.createSuiteDescription(this.getName(), this.classAnnotations());
        List<Method> testMethods = this.testMethods;
        for (Method method : testMethods) {
            spec.addChild(this.methodDescription(method));
        }
        return spec;
    }

    protected Annotation[] classAnnotations() {
        return this.testClass.getJavaClass().getAnnotations();
    }

    protected String getName() {
        return this.getTestClass().getName();
    }

    protected Object createTest() throws Exception {
        return this.getTestClass().getConstructor().newInstance(new Object[0]);
    }

    protected void invokeTestMethod(Method method, RunNotifier notifier) {
        Object test;
        Description description = this.methodDescription(method);
        try {
            test = this.createTest();
        }
        catch (InvocationTargetException e) {
            this.testAborted(notifier, description, e.getCause());
            return;
        }
        catch (Exception e) {
            this.testAborted(notifier, description, e);
            return;
        }
        TestMethod testMethod = this.wrapMethod(method);
        new MethodRoadie(test, testMethod, notifier, description).run();
    }

    private void testAborted(RunNotifier notifier, Description description, Throwable e) {
        notifier.fireTestStarted(description);
        notifier.fireTestFailure(new Failure(description, e));
        notifier.fireTestFinished(description);
    }

    protected TestMethod wrapMethod(Method method) {
        return new TestMethod(method, this.testClass);
    }

    protected String testName(Method method) {
        return method.getName();
    }

    protected Description methodDescription(Method method) {
        return Description.createTestDescription(this.getTestClass().getJavaClass(), this.testName(method), this.testAnnotations(method));
    }

    protected Annotation[] testAnnotations(Method method) {
        return method.getAnnotations();
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        Iterator<Method> iter = this.testMethods.iterator();
        while (iter.hasNext()) {
            Method method = iter.next();
            if (filter.shouldRun(this.methodDescription(method))) continue;
            iter.remove();
        }
        if (this.testMethods.isEmpty()) {
            throw new NoTestsRemainException();
        }
    }

    @Override
    public void sort(final Sorter sorter) {
        Collections.sort(this.testMethods, new Comparator<Method>(){

            @Override
            public int compare(Method o1, Method o2) {
                return sorter.compare(JUnit4ClassRunner.this.methodDescription(o1), JUnit4ClassRunner.this.methodDescription(o2));
            }
        });
    }

    protected TestClass getTestClass() {
        return this.testClass;
    }
}

