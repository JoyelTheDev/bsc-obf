/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.rules.RuleMemberValidator;
import org.junit.internal.runners.statements.ExpectException;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.MethodRule;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class BlockJUnit4ClassRunner
extends ParentRunner<FrameworkMethod> {
    private final ConcurrentHashMap<FrameworkMethod, Description> methodDescriptions = new ConcurrentHashMap();

    public BlockJUnit4ClassRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        Description description = this.describeChild(method);
        if (this.isIgnored(method)) {
            notifier.fireTestIgnored(description);
        } else {
            this.runLeaf(this.methodBlock(method), description, notifier);
        }
    }

    @Override
    protected boolean isIgnored(FrameworkMethod child) {
        return child.getAnnotation(Ignore.class) != null;
    }

    @Override
    protected Description describeChild(FrameworkMethod method) {
        Description description = this.methodDescriptions.get(method);
        if (description == null) {
            description = Description.createTestDescription(this.getTestClass().getJavaClass(), this.testName(method), method.getAnnotations());
            this.methodDescriptions.putIfAbsent(method, description);
        }
        return description;
    }

    @Override
    protected List<FrameworkMethod> getChildren() {
        return this.computeTestMethods();
    }

    protected List<FrameworkMethod> computeTestMethods() {
        return this.getTestClass().getAnnotatedMethods(Test.class);
    }

    @Override
    protected void collectInitializationErrors(List<Throwable> errors) {
        super.collectInitializationErrors(errors);
        this.validateNoNonStaticInnerClass(errors);
        this.validateConstructor(errors);
        this.validateInstanceMethods(errors);
        this.validateFields(errors);
        this.validateMethods(errors);
    }

    protected void validateNoNonStaticInnerClass(List<Throwable> errors) {
        if (this.getTestClass().isANonStaticInnerClass()) {
            String gripe = "The inner class " + this.getTestClass().getName() + " is not static.";
            errors.add(new Exception(gripe));
        }
    }

    protected void validateConstructor(List<Throwable> errors) {
        this.validateOnlyOneConstructor(errors);
        this.validateZeroArgConstructor(errors);
    }

    protected void validateOnlyOneConstructor(List<Throwable> errors) {
        if (!this.hasOneConstructor()) {
            String gripe = "Test class should have exactly one public constructor";
            errors.add(new Exception(gripe));
        }
    }

    protected void validateZeroArgConstructor(List<Throwable> errors) {
        if (!this.getTestClass().isANonStaticInnerClass() && this.hasOneConstructor() && this.getTestClass().getOnlyConstructor().getParameterTypes().length != 0) {
            String gripe = "Test class should have exactly one public zero-argument constructor";
            errors.add(new Exception(gripe));
        }
    }

    private boolean hasOneConstructor() {
        return this.getTestClass().getJavaClass().getConstructors().length == 1;
    }

    @Deprecated
    protected void validateInstanceMethods(List<Throwable> errors) {
        this.validatePublicVoidNoArgMethods(After.class, false, errors);
        this.validatePublicVoidNoArgMethods(Before.class, false, errors);
        this.validateTestMethods(errors);
        if (this.computeTestMethods().size() == 0) {
            errors.add(new Exception("No runnable methods"));
        }
    }

    protected void validateFields(List<Throwable> errors) {
        RuleMemberValidator.RULE_VALIDATOR.validate(this.getTestClass(), errors);
    }

    private void validateMethods(List<Throwable> errors) {
        RuleMemberValidator.RULE_METHOD_VALIDATOR.validate(this.getTestClass(), errors);
    }

    protected void validateTestMethods(List<Throwable> errors) {
        this.validatePublicVoidNoArgMethods(Test.class, false, errors);
    }

    protected Object createTest() throws Exception {
        return this.getTestClass().getOnlyConstructor().newInstance(new Object[0]);
    }

    protected String testName(FrameworkMethod method) {
        return method.getName();
    }

    protected Statement methodBlock(FrameworkMethod method) {
        Object test;
        try {
            test = new ReflectiveCallable(){

                protected Object runReflectiveCall() throws Throwable {
                    return BlockJUnit4ClassRunner.this.createTest();
                }
            }.run();
        }
        catch (Throwable e) {
            return new Fail(e);
        }
        Statement statement = this.methodInvoker(method, test);
        statement = this.possiblyExpectingExceptions(method, test, statement);
        statement = this.withPotentialTimeout(method, test, statement);
        statement = this.withBefores(method, test, statement);
        statement = this.withAfters(method, test, statement);
        statement = this.withRules(method, test, statement);
        return statement;
    }

    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        return new InvokeMethod(method, test);
    }

    protected Statement possiblyExpectingExceptions(FrameworkMethod method, Object test, Statement next) {
        Test annotation = method.getAnnotation(Test.class);
        return this.expectsException(annotation) ? new ExpectException(next, this.getExpectedException(annotation)) : next;
    }

    @Deprecated
    protected Statement withPotentialTimeout(FrameworkMethod method, Object test, Statement next) {
        long timeout = this.getTimeout(method.getAnnotation(Test.class));
        if (timeout <= 0L) {
            return next;
        }
        return FailOnTimeout.builder().withTimeout(timeout, TimeUnit.MILLISECONDS).build(next);
    }

    protected Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
        List<FrameworkMethod> befores = this.getTestClass().getAnnotatedMethods(Before.class);
        return befores.isEmpty() ? statement : new RunBefores(statement, befores, target);
    }

    protected Statement withAfters(FrameworkMethod method, Object target, Statement statement) {
        List<FrameworkMethod> afters = this.getTestClass().getAnnotatedMethods(After.class);
        return afters.isEmpty() ? statement : new RunAfters(statement, afters, target);
    }

    private Statement withRules(FrameworkMethod method, Object target, Statement statement) {
        List<TestRule> testRules = this.getTestRules(target);
        Statement result = statement;
        result = this.withMethodRules(method, testRules, target, result);
        result = this.withTestRules(method, testRules, result);
        return result;
    }

    private Statement withMethodRules(FrameworkMethod method, List<TestRule> testRules, Object target, Statement result) {
        for (MethodRule each : this.getMethodRules(target)) {
            if (testRules.contains(each)) continue;
            result = each.apply(result, method, target);
        }
        return result;
    }

    private List<MethodRule> getMethodRules(Object target) {
        return this.rules(target);
    }

    protected List<MethodRule> rules(Object target) {
        List<MethodRule> rules = this.getTestClass().getAnnotatedMethodValues(target, Rule.class, MethodRule.class);
        rules.addAll(this.getTestClass().getAnnotatedFieldValues(target, Rule.class, MethodRule.class));
        return rules;
    }

    private Statement withTestRules(FrameworkMethod method, List<TestRule> testRules, Statement statement) {
        return testRules.isEmpty() ? statement : new RunRules(statement, testRules, this.describeChild(method));
    }

    protected List<TestRule> getTestRules(Object target) {
        List<TestRule> result = this.getTestClass().getAnnotatedMethodValues(target, Rule.class, TestRule.class);
        result.addAll(this.getTestClass().getAnnotatedFieldValues(target, Rule.class, TestRule.class));
        return result;
    }

    private Class<? extends Throwable> getExpectedException(Test annotation) {
        if (annotation == null || annotation.expected() == Test.None.class) {
            return null;
        }
        return annotation.expected();
    }

    private boolean expectsException(Test annotation) {
        return this.getExpectedException(annotation) != null;
    }

    private long getTimeout(Test annotation) {
        if (annotation == null) {
            return 0L;
        }
        return annotation.timeout();
    }
}

