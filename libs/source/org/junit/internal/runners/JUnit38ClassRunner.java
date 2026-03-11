/*
 * Decompiled with CFR 0.152.
 */
package org.junit.internal.runners;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import junit.extensions.TestDecorator;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.junit.runner.Describable;
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
public class JUnit38ClassRunner
extends Runner
implements Filterable,
Sortable {
    private volatile Test test;

    public JUnit38ClassRunner(Class<?> klass) {
        this(new TestSuite((Class<?>)klass.asSubclass(TestCase.class)));
    }

    public JUnit38ClassRunner(Test test) {
        this.setTest(test);
    }

    @Override
    public void run(RunNotifier notifier) {
        TestResult result = new TestResult();
        result.addListener(this.createAdaptingListener(notifier));
        this.getTest().run(result);
    }

    public TestListener createAdaptingListener(RunNotifier notifier) {
        return new OldTestClassAdaptingListener(notifier);
    }

    @Override
    public Description getDescription() {
        return JUnit38ClassRunner.makeDescription(this.getTest());
    }

    private static Description makeDescription(Test test) {
        if (test instanceof TestCase) {
            TestCase tc = (TestCase)test;
            return Description.createTestDescription(tc.getClass(), tc.getName(), JUnit38ClassRunner.getAnnotations(tc));
        }
        if (test instanceof TestSuite) {
            TestSuite ts = (TestSuite)test;
            String name = ts.getName() == null ? JUnit38ClassRunner.createSuiteDescription(ts) : ts.getName();
            Description description = Description.createSuiteDescription(name, new Annotation[0]);
            int n = ts.testCount();
            for (int i = 0; i < n; ++i) {
                Description made = JUnit38ClassRunner.makeDescription(ts.testAt(i));
                description.addChild(made);
            }
            return description;
        }
        if (test instanceof Describable) {
            Describable adapter = (Describable)((Object)test);
            return adapter.getDescription();
        }
        if (test instanceof TestDecorator) {
            TestDecorator decorator = (TestDecorator)test;
            return JUnit38ClassRunner.makeDescription(decorator.getTest());
        }
        return Description.createSuiteDescription(test.getClass());
    }

    private static Annotation[] getAnnotations(TestCase test) {
        try {
            Method m = test.getClass().getMethod(test.getName(), new Class[0]);
            return m.getDeclaredAnnotations();
        }
        catch (SecurityException e) {
        }
        catch (NoSuchMethodException noSuchMethodException) {
            // empty catch block
        }
        return new Annotation[0];
    }

    private static String createSuiteDescription(TestSuite ts) {
        int count = ts.countTestCases();
        String example = count == 0 ? "" : String.format(" [example: %s]", ts.testAt(0));
        return String.format("TestSuite with %s tests%s", count, example);
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        if (this.getTest() instanceof Filterable) {
            Filterable adapter = (Filterable)((Object)this.getTest());
            adapter.filter(filter);
        } else if (this.getTest() instanceof TestSuite) {
            TestSuite suite = (TestSuite)this.getTest();
            TestSuite filtered = new TestSuite(suite.getName());
            int n = suite.testCount();
            for (int i = 0; i < n; ++i) {
                Test test = suite.testAt(i);
                if (!filter.shouldRun(JUnit38ClassRunner.makeDescription(test))) continue;
                filtered.addTest(test);
            }
            this.setTest(filtered);
            if (filtered.testCount() == 0) {
                throw new NoTestsRemainException();
            }
        }
    }

    @Override
    public void sort(Sorter sorter) {
        if (this.getTest() instanceof Sortable) {
            Sortable adapter = (Sortable)((Object)this.getTest());
            adapter.sort(sorter);
        }
    }

    private void setTest(Test test) {
        this.test = test;
    }

    private Test getTest() {
        return this.test;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static final class OldTestClassAdaptingListener
    implements TestListener {
        private final RunNotifier notifier;

        private OldTestClassAdaptingListener(RunNotifier notifier) {
            this.notifier = notifier;
        }

        @Override
        public void endTest(Test test) {
            this.notifier.fireTestFinished(this.asDescription(test));
        }

        @Override
        public void startTest(Test test) {
            this.notifier.fireTestStarted(this.asDescription(test));
        }

        @Override
        public void addError(Test test, Throwable e) {
            Failure failure = new Failure(this.asDescription(test), e);
            this.notifier.fireTestFailure(failure);
        }

        private Description asDescription(Test test) {
            if (test instanceof Describable) {
                Describable facade = (Describable)((Object)test);
                return facade.getDescription();
            }
            return Description.createTestDescription(this.getEffectiveClass(test), this.getName(test));
        }

        private Class<? extends Test> getEffectiveClass(Test test) {
            return test.getClass();
        }

        private String getName(Test test) {
            if (test instanceof TestCase) {
                return ((TestCase)test).getName();
            }
            return test.toString();
        }

        @Override
        public void addFailure(Test test, AssertionFailedError t) {
            this.addError(test, (Throwable)((Object)t));
        }
    }
}

