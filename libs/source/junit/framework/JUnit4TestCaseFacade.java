/*
 * Decompiled with CFR 0.152.
 */
package junit.framework;

import junit.framework.Test;
import junit.framework.TestResult;
import org.junit.runner.Describable;
import org.junit.runner.Description;

public class JUnit4TestCaseFacade
implements Test,
Describable {
    private final Description fDescription;

    JUnit4TestCaseFacade(Description description) {
        this.fDescription = description;
    }

    public String toString() {
        return this.getDescription().toString();
    }

    public int countTestCases() {
        return 1;
    }

    public void run(TestResult result) {
        throw new RuntimeException("This test stub created only for informational purposes.");
    }

    public Description getDescription() {
        return this.fDescription;
    }
}

