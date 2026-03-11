/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.max;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junit.framework.TestSuite;
import org.junit.experimental.max.MaxHistory;
import org.junit.internal.requests.SortingRequest;
import org.junit.internal.runners.ErrorReportingRunner;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class MaxCore {
    private static final String MALFORMED_JUNIT_3_TEST_CLASS_PREFIX = "malformed JUnit 3 test class: ";
    private final MaxHistory history;

    @Deprecated
    public static MaxCore forFolder(String folderName) {
        return MaxCore.storedLocally(new File(folderName));
    }

    public static MaxCore storedLocally(File storedResults) {
        return new MaxCore(storedResults);
    }

    private MaxCore(File storedResults) {
        this.history = MaxHistory.forFolder(storedResults);
    }

    public Result run(Class<?> testClass) {
        return this.run(Request.aClass(testClass));
    }

    public Result run(Request request) {
        return this.run(request, new JUnitCore());
    }

    public Result run(Request request, JUnitCore core) {
        core.addListener(this.history.listener());
        return core.run(this.sortRequest(request).getRunner());
    }

    public Request sortRequest(Request request) {
        if (request instanceof SortingRequest) {
            return request;
        }
        List<Description> leaves = this.findLeaves(request);
        Collections.sort(leaves, this.history.testComparator());
        return this.constructLeafRequest(leaves);
    }

    private Request constructLeafRequest(List<Description> leaves) {
        final ArrayList<Runner> runners = new ArrayList<Runner>();
        for (Description each : leaves) {
            runners.add(this.buildRunner(each));
        }
        return new Request(){

            public Runner getRunner() {
                try {
                    return new Suite((Class)null, runners){};
                }
                catch (InitializationError e) {
                    return new ErrorReportingRunner(null, e);
                }
            }
        };
    }

    private Runner buildRunner(Description each) {
        if (each.toString().equals("TestSuite with 0 tests")) {
            return Suite.emptySuite();
        }
        if (each.toString().startsWith(MALFORMED_JUNIT_3_TEST_CLASS_PREFIX)) {
            return new JUnit38ClassRunner(new TestSuite(this.getMalformedTestClass(each)));
        }
        Class<?> type = each.getTestClass();
        if (type == null) {
            throw new RuntimeException("Can't build a runner from description [" + each + "]");
        }
        String methodName = each.getMethodName();
        if (methodName == null) {
            return Request.aClass(type).getRunner();
        }
        return Request.method(type, methodName).getRunner();
    }

    private Class<?> getMalformedTestClass(Description each) {
        try {
            return Class.forName(each.toString().replace(MALFORMED_JUNIT_3_TEST_CLASS_PREFIX, ""));
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }

    public List<Description> sortedLeavesForTest(Request request) {
        return this.findLeaves(this.sortRequest(request));
    }

    private List<Description> findLeaves(Request request) {
        ArrayList<Description> results = new ArrayList<Description>();
        this.findLeaves(null, request.getRunner().getDescription(), results);
        return results;
    }

    private void findLeaves(Description parent, Description description, List<Description> results) {
        if (description.getChildren().isEmpty()) {
            if (description.toString().equals("warning(junit.framework.TestSuite$1)")) {
                results.add(Description.createSuiteDescription(MALFORMED_JUNIT_3_TEST_CLASS_PREFIX + parent, new Annotation[0]));
            } else {
                results.add(description);
            }
        } else {
            for (Description each : description.getChildren()) {
                this.findLeaves(description, each, results);
            }
        }
    }
}

