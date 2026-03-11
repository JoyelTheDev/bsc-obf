/*
 * Decompiled with CFR 0.152.
 */
package junit.framework;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import org.junit.internal.MethodSorter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class TestSuite
implements Test {
    private String fName;
    private Vector<Test> fTests = new Vector(10);

    public static Test createTest(Class<?> theClass, String name) {
        Object test;
        Constructor<?> constructor;
        try {
            constructor = TestSuite.getTestConstructor(theClass);
        }
        catch (NoSuchMethodException e) {
            return TestSuite.warning("Class " + theClass.getName() + " has no public constructor TestCase(String name) or TestCase()");
        }
        try {
            if (constructor.getParameterTypes().length == 0) {
                test = constructor.newInstance(new Object[0]);
                if (test instanceof TestCase) {
                    ((TestCase)test).setName(name);
                }
            } else {
                test = constructor.newInstance(name);
            }
        }
        catch (InstantiationException e) {
            return TestSuite.warning("Cannot instantiate test case: " + name + " (" + TestSuite.exceptionToString(e) + ")");
        }
        catch (InvocationTargetException e) {
            return TestSuite.warning("Exception in constructor: " + name + " (" + TestSuite.exceptionToString(e.getTargetException()) + ")");
        }
        catch (IllegalAccessException e) {
            return TestSuite.warning("Cannot access test case: " + name + " (" + TestSuite.exceptionToString(e) + ")");
        }
        return (Test)test;
    }

    public static Constructor<?> getTestConstructor(Class<?> theClass) throws NoSuchMethodException {
        try {
            return theClass.getConstructor(String.class);
        }
        catch (NoSuchMethodException noSuchMethodException) {
            return theClass.getConstructor(new Class[0]);
        }
    }

    public static Test warning(final String message) {
        return new TestCase("warning"){

            protected void runTest() {
                1.fail(message);
            }
        };
    }

    private static String exceptionToString(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        e.printStackTrace(writer);
        return stringWriter.toString();
    }

    public TestSuite() {
    }

    public TestSuite(Class<?> theClass) {
        this.addTestsFromTestCase(theClass);
    }

    private void addTestsFromTestCase(Class<?> theClass) {
        this.fName = theClass.getName();
        try {
            TestSuite.getTestConstructor(theClass);
        }
        catch (NoSuchMethodException e) {
            this.addTest(TestSuite.warning("Class " + theClass.getName() + " has no public constructor TestCase(String name) or TestCase()"));
            return;
        }
        if (!Modifier.isPublic(theClass.getModifiers())) {
            this.addTest(TestSuite.warning("Class " + theClass.getName() + " is not public"));
            return;
        }
        Class<?> superClass = theClass;
        ArrayList<String> names = new ArrayList<String>();
        while (Test.class.isAssignableFrom(superClass)) {
            for (Method each : MethodSorter.getDeclaredMethods(superClass)) {
                this.addTestMethod(each, names, theClass);
            }
            superClass = superClass.getSuperclass();
        }
        if (this.fTests.size() == 0) {
            this.addTest(TestSuite.warning("No tests found in " + theClass.getName()));
        }
    }

    public TestSuite(Class<? extends TestCase> theClass, String name) {
        this((Class<?>)theClass);
        this.setName(name);
    }

    public TestSuite(String name) {
        this.setName(name);
    }

    public TestSuite(Class<?> ... classes) {
        for (Class<?> each : classes) {
            this.addTest(this.testCaseForClass(each));
        }
    }

    private Test testCaseForClass(Class<?> each) {
        if (TestCase.class.isAssignableFrom(each)) {
            return new TestSuite((Class<?>)each.asSubclass(TestCase.class));
        }
        return TestSuite.warning(each.getCanonicalName() + " does not extend TestCase");
    }

    public TestSuite(Class<? extends TestCase>[] classes, String name) {
        this(classes);
        this.setName(name);
    }

    public void addTest(Test test) {
        this.fTests.add(test);
    }

    public void addTestSuite(Class<? extends TestCase> testClass) {
        this.addTest(new TestSuite((Class<?>)testClass));
    }

    @Override
    public int countTestCases() {
        int count = 0;
        for (Test each : this.fTests) {
            count += each.countTestCases();
        }
        return count;
    }

    public String getName() {
        return this.fName;
    }

    @Override
    public void run(TestResult result) {
        for (Test each : this.fTests) {
            if (result.shouldStop()) break;
            this.runTest(each, result);
        }
    }

    public void runTest(Test test, TestResult result) {
        test.run(result);
    }

    public void setName(String name) {
        this.fName = name;
    }

    public Test testAt(int index) {
        return this.fTests.get(index);
    }

    public int testCount() {
        return this.fTests.size();
    }

    public Enumeration<Test> tests() {
        return this.fTests.elements();
    }

    public String toString() {
        if (this.getName() != null) {
            return this.getName();
        }
        return super.toString();
    }

    private void addTestMethod(Method m, List<String> names, Class<?> theClass) {
        String name = m.getName();
        if (names.contains(name)) {
            return;
        }
        if (!this.isPublicTestMethod(m)) {
            if (this.isTestMethod(m)) {
                this.addTest(TestSuite.warning("Test method isn't public: " + m.getName() + "(" + theClass.getCanonicalName() + ")"));
            }
            return;
        }
        names.add(name);
        this.addTest(TestSuite.createTest(theClass, name));
    }

    private boolean isPublicTestMethod(Method m) {
        return this.isTestMethod(m) && Modifier.isPublic(m.getModifiers());
    }

    private boolean isTestMethod(Method m) {
        return m.getParameterTypes().length == 0 && m.getName().startsWith("test") && m.getReturnType().equals(Void.TYPE);
    }
}

