/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.EachTestNotifier;
import org.junit.internal.runners.rules.RuleMemberValidator;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerScheduler;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.junit.validator.AnnotationsValidator;
import org.junit.validator.PublicClassValidator;
import org.junit.validator.TestClassValidator;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public abstract class ParentRunner<T>
extends Runner
implements Filterable,
Sortable {
    private static final List<TestClassValidator> VALIDATORS = Arrays.asList(new AnnotationsValidator(), new PublicClassValidator());
    private final Object childrenLock = new Object();
    private final TestClass testClass;
    private volatile Collection<T> filteredChildren = null;
    private volatile RunnerScheduler scheduler = new RunnerScheduler(){

        public void schedule(Runnable childStatement) {
            childStatement.run();
        }

        public void finished() {
        }
    };

    protected ParentRunner(Class<?> testClass) throws InitializationError {
        this.testClass = this.createTestClass(testClass);
        this.validate();
    }

    protected TestClass createTestClass(Class<?> testClass) {
        return new TestClass(testClass);
    }

    protected abstract List<T> getChildren();

    protected abstract Description describeChild(T var1);

    protected abstract void runChild(T var1, RunNotifier var2);

    protected void collectInitializationErrors(List<Throwable> errors) {
        this.validatePublicVoidNoArgMethods(BeforeClass.class, true, errors);
        this.validatePublicVoidNoArgMethods(AfterClass.class, true, errors);
        this.validateClassRules(errors);
        this.applyValidators(errors);
    }

    private void applyValidators(List<Throwable> errors) {
        if (this.getTestClass().getJavaClass() != null) {
            for (TestClassValidator each : VALIDATORS) {
                errors.addAll(each.validateTestClass(this.getTestClass()));
            }
        }
    }

    protected void validatePublicVoidNoArgMethods(Class<? extends Annotation> annotation, boolean isStatic, List<Throwable> errors) {
        List<FrameworkMethod> methods = this.getTestClass().getAnnotatedMethods(annotation);
        for (FrameworkMethod eachTestMethod : methods) {
            eachTestMethod.validatePublicVoidNoArg(isStatic, errors);
        }
    }

    private void validateClassRules(List<Throwable> errors) {
        RuleMemberValidator.CLASS_RULE_VALIDATOR.validate(this.getTestClass(), errors);
        RuleMemberValidator.CLASS_RULE_METHOD_VALIDATOR.validate(this.getTestClass(), errors);
    }

    protected Statement classBlock(RunNotifier notifier) {
        Statement statement = this.childrenInvoker(notifier);
        if (!this.areAllChildrenIgnored()) {
            statement = this.withBeforeClasses(statement);
            statement = this.withAfterClasses(statement);
            statement = this.withClassRules(statement);
        }
        return statement;
    }

    private boolean areAllChildrenIgnored() {
        for (T child : this.getFilteredChildren()) {
            if (this.isIgnored(child)) continue;
            return false;
        }
        return true;
    }

    protected Statement withBeforeClasses(Statement statement) {
        List<FrameworkMethod> befores = this.testClass.getAnnotatedMethods(BeforeClass.class);
        return befores.isEmpty() ? statement : new RunBefores(statement, befores, null);
    }

    protected Statement withAfterClasses(Statement statement) {
        List<FrameworkMethod> afters = this.testClass.getAnnotatedMethods(AfterClass.class);
        return afters.isEmpty() ? statement : new RunAfters(statement, afters, null);
    }

    private Statement withClassRules(Statement statement) {
        List<TestRule> classRules = this.classRules();
        return classRules.isEmpty() ? statement : new RunRules(statement, classRules, this.getDescription());
    }

    protected List<TestRule> classRules() {
        List<TestRule> result = this.testClass.getAnnotatedMethodValues(null, ClassRule.class, TestRule.class);
        result.addAll(this.testClass.getAnnotatedFieldValues(null, ClassRule.class, TestRule.class));
        return result;
    }

    protected Statement childrenInvoker(final RunNotifier notifier) {
        return new Statement(){

            public void evaluate() {
                ParentRunner.this.runChildren(notifier);
            }
        };
    }

    protected boolean isIgnored(T child) {
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void runChildren(final RunNotifier notifier) {
        RunnerScheduler currentScheduler = this.scheduler;
        try {
            for (final T each : this.getFilteredChildren()) {
                currentScheduler.schedule(new Runnable(){

                    public void run() {
                        ParentRunner.this.runChild(each, notifier);
                    }
                });
            }
        }
        finally {
            currentScheduler.finished();
        }
    }

    protected String getName() {
        return this.testClass.getName();
    }

    public final TestClass getTestClass() {
        return this.testClass;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected final void runLeaf(Statement statement, Description description, RunNotifier notifier) {
        EachTestNotifier eachNotifier = new EachTestNotifier(notifier, description);
        eachNotifier.fireTestStarted();
        try {
            statement.evaluate();
        }
        catch (AssumptionViolatedException e) {
            eachNotifier.addFailedAssumption(e);
        }
        catch (Throwable e) {
            eachNotifier.addFailure(e);
        }
        finally {
            eachNotifier.fireTestFinished();
        }
    }

    protected Annotation[] getRunnerAnnotations() {
        return this.testClass.getAnnotations();
    }

    @Override
    public Description getDescription() {
        Description description = Description.createSuiteDescription(this.getName(), this.getRunnerAnnotations());
        for (T child : this.getFilteredChildren()) {
            description.addChild(this.describeChild(child));
        }
        return description;
    }

    @Override
    public void run(RunNotifier notifier) {
        EachTestNotifier testNotifier = new EachTestNotifier(notifier, this.getDescription());
        try {
            Statement statement = this.classBlock(notifier);
            statement.evaluate();
        }
        catch (AssumptionViolatedException e) {
            testNotifier.addFailedAssumption(e);
        }
        catch (StoppedByUserException e) {
            throw e;
        }
        catch (Throwable e) {
            testNotifier.addFailure(e);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        Object object = this.childrenLock;
        synchronized (object) {
            ArrayList<T> children = new ArrayList<T>(this.getFilteredChildren());
            Iterator iter = children.iterator();
            while (iter.hasNext()) {
                Object each = iter.next();
                if (this.shouldRun(filter, each)) {
                    try {
                        filter.apply(each);
                    }
                    catch (NoTestsRemainException e) {
                        iter.remove();
                    }
                    continue;
                }
                iter.remove();
            }
            this.filteredChildren = Collections.unmodifiableCollection(children);
            if (this.filteredChildren.isEmpty()) {
                throw new NoTestsRemainException();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void sort(Sorter sorter) {
        Object object = this.childrenLock;
        synchronized (object) {
            for (T each : this.getFilteredChildren()) {
                sorter.apply(each);
            }
            ArrayList<T> sortedChildren = new ArrayList<T>(this.getFilteredChildren());
            Collections.sort(sortedChildren, this.comparator(sorter));
            this.filteredChildren = Collections.unmodifiableCollection(sortedChildren);
        }
    }

    private void validate() throws InitializationError {
        ArrayList<Throwable> errors = new ArrayList<Throwable>();
        this.collectInitializationErrors(errors);
        if (!errors.isEmpty()) {
            throw new InitializationError(errors);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Collection<T> getFilteredChildren() {
        if (this.filteredChildren == null) {
            Object object = this.childrenLock;
            synchronized (object) {
                if (this.filteredChildren == null) {
                    this.filteredChildren = Collections.unmodifiableCollection(this.getChildren());
                }
            }
        }
        return this.filteredChildren;
    }

    private boolean shouldRun(Filter filter, T each) {
        return filter.shouldRun(this.describeChild(each));
    }

    private Comparator<? super T> comparator(final Sorter sorter) {
        return new Comparator<T>(){

            @Override
            public int compare(T o1, T o2) {
                return sorter.compare(ParentRunner.this.describeChild(o1), ParentRunner.this.describeChild(o2));
            }
        };
    }

    public void setScheduler(RunnerScheduler scheduler) {
        this.scheduler = scheduler;
    }
}

