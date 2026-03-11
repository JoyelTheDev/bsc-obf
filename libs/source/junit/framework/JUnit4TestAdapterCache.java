/*
 * Decompiled with CFR 0.152.
 */
package junit.framework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import junit.framework.JUnit4TestAdapter;
import junit.framework.JUnit4TestCaseFacade;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class JUnit4TestAdapterCache
extends HashMap<Description, Test> {
    private static final long serialVersionUID = 1L;
    private static final JUnit4TestAdapterCache fInstance = new JUnit4TestAdapterCache();

    public static JUnit4TestAdapterCache getDefault() {
        return fInstance;
    }

    public Test asTest(Description description) {
        if (description.isSuite()) {
            return this.createTest(description);
        }
        if (!this.containsKey(description)) {
            this.put(description, this.createTest(description));
        }
        return (Test)this.get(description);
    }

    Test createTest(Description description) {
        if (description.isTest()) {
            return new JUnit4TestCaseFacade(description);
        }
        TestSuite suite = new TestSuite(description.getDisplayName());
        for (Description child : description.getChildren()) {
            suite.addTest(this.asTest(child));
        }
        return suite;
    }

    public RunNotifier getNotifier(final TestResult result, JUnit4TestAdapter adapter) {
        RunNotifier notifier = new RunNotifier();
        notifier.addListener(new RunListener(){

            public void testFailure(Failure failure) throws Exception {
                result.addError(JUnit4TestAdapterCache.this.asTest(failure.getDescription()), failure.getException());
            }

            public void testFinished(Description description) throws Exception {
                result.endTest(JUnit4TestAdapterCache.this.asTest(description));
            }

            public void testStarted(Description description) throws Exception {
                result.startTest(JUnit4TestAdapterCache.this.asTest(description));
            }
        });
        return notifier;
    }

    public List<Test> asTestList(Description description) {
        if (description.isTest()) {
            return Arrays.asList(this.asTest(description));
        }
        ArrayList<Test> returnThis = new ArrayList<Test>();
        for (Description child : description.getChildren()) {
            returnThis.add(this.asTest(child));
        }
        return returnThis;
    }
}

