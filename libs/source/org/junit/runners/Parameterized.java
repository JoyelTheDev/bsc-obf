/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParametersFactory;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Parameterized
extends Suite {
    private static final ParametersRunnerFactory DEFAULT_FACTORY = new BlockJUnit4ClassRunnerWithParametersFactory();
    private static final List<Runner> NO_RUNNERS = Collections.emptyList();
    private final List<Runner> runners;

    public Parameterized(Class<?> klass) throws Throwable {
        super(klass, NO_RUNNERS);
        ParametersRunnerFactory runnerFactory = this.getParametersRunnerFactory(klass);
        Parameters parameters = this.getParametersMethod().getAnnotation(Parameters.class);
        this.runners = Collections.unmodifiableList(this.createRunnersForParameters(this.allParameters(), parameters.name(), runnerFactory));
    }

    private ParametersRunnerFactory getParametersRunnerFactory(Class<?> klass) throws InstantiationException, IllegalAccessException {
        UseParametersRunnerFactory annotation = klass.getAnnotation(UseParametersRunnerFactory.class);
        if (annotation == null) {
            return DEFAULT_FACTORY;
        }
        Class<? extends ParametersRunnerFactory> factoryClass = annotation.value();
        return factoryClass.newInstance();
    }

    @Override
    protected List<Runner> getChildren() {
        return this.runners;
    }

    private TestWithParameters createTestWithNotNormalizedParameters(String pattern, int index, Object parametersOrSingleParameter) {
        Object[] objectArray;
        if (parametersOrSingleParameter instanceof Object[]) {
            objectArray = (Object[])parametersOrSingleParameter;
        } else {
            Object[] objectArray2 = new Object[1];
            objectArray = objectArray2;
            objectArray2[0] = parametersOrSingleParameter;
        }
        Object[] parameters = objectArray;
        return Parameterized.createTestWithParameters(this.getTestClass(), pattern, index, parameters);
    }

    private Iterable<Object> allParameters() throws Throwable {
        Object parameters = this.getParametersMethod().invokeExplosively(null, new Object[0]);
        if (parameters instanceof Iterable) {
            return (Iterable)parameters;
        }
        if (parameters instanceof Object[]) {
            return Arrays.asList((Object[])parameters);
        }
        throw this.parametersMethodReturnedWrongType();
    }

    private FrameworkMethod getParametersMethod() throws Exception {
        List<FrameworkMethod> methods = this.getTestClass().getAnnotatedMethods(Parameters.class);
        for (FrameworkMethod each : methods) {
            if (!each.isStatic() || !each.isPublic()) continue;
            return each;
        }
        throw new Exception("No public static parameters method on class " + this.getTestClass().getName());
    }

    private List<Runner> createRunnersForParameters(Iterable<Object> allParameters, String namePattern, ParametersRunnerFactory runnerFactory) throws InitializationError, Exception {
        try {
            List<TestWithParameters> tests = this.createTestsForParameters(allParameters, namePattern);
            ArrayList<Runner> runners = new ArrayList<Runner>();
            for (TestWithParameters test : tests) {
                runners.add(runnerFactory.createRunnerForTestWithParameters(test));
            }
            return runners;
        }
        catch (ClassCastException e) {
            throw this.parametersMethodReturnedWrongType();
        }
    }

    private List<TestWithParameters> createTestsForParameters(Iterable<Object> allParameters, String namePattern) throws Exception {
        int i = 0;
        ArrayList<TestWithParameters> children = new ArrayList<TestWithParameters>();
        for (Object parametersOfSingleTest : allParameters) {
            children.add(this.createTestWithNotNormalizedParameters(namePattern, i++, parametersOfSingleTest));
        }
        return children;
    }

    private Exception parametersMethodReturnedWrongType() throws Exception {
        String className = this.getTestClass().getName();
        String methodName = this.getParametersMethod().getName();
        String message = MessageFormat.format("{0}.{1}() must return an Iterable of arrays.", className, methodName);
        return new Exception(message);
    }

    private static TestWithParameters createTestWithParameters(TestClass testClass, String pattern, int index, Object[] parameters) {
        String finalPattern = pattern.replaceAll("\\{index\\}", Integer.toString(index));
        String name = MessageFormat.format(finalPattern, parameters);
        return new TestWithParameters("[" + name + "]", testClass, Arrays.asList(parameters));
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    @Retention(value=RetentionPolicy.RUNTIME)
    @Inherited
    @Target(value={ElementType.TYPE})
    public static @interface UseParametersRunnerFactory {
        public Class<? extends ParametersRunnerFactory> value() default BlockJUnit4ClassRunnerWithParametersFactory.class;
    }

    @Retention(value=RetentionPolicy.RUNTIME)
    @Target(value={ElementType.FIELD})
    public static @interface Parameter {
        public int value() default 0;
    }

    @Retention(value=RetentionPolicy.RUNTIME)
    @Target(value={ElementType.METHOD})
    public static @interface Parameters {
        public String name() default "{index}";
    }
}

