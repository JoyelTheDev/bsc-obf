/*
 * Decompiled with CFR 0.152.
 */
package junit.framework;

import java.util.List;
import junit.framework.JUnit4TestAdapterCache;
import junit.framework.Test;
import junit.framework.TestResult;
import org.junit.Ignore;
import org.junit.runner.Describable;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class JUnit4TestAdapter
implements Test,
Filterable,
Sortable,
Describable {
    private final Class<?> fNewTestClass;
    private final Runner fRunner;
    private final JUnit4TestAdapterCache fCache;

    public JUnit4TestAdapter(Class<?> newTestClass) {
        this(newTestClass, JUnit4TestAdapterCache.getDefault());
    }

    public JUnit4TestAdapter(Class<?> newTestClass, JUnit4TestAdapterCache cache) {
        this.fCache = cache;
        this.fNewTestClass = newTestClass;
        this.fRunner = Request.classWithoutSuiteMethod(newTestClass).getRunner();
    }

    @Override
    public int countTestCases() {
        return this.fRunner.testCount();
    }

    @Override
    public void run(TestResult result) {
        this.fRunner.run(this.fCache.getNotifier(result, this));
    }

    public List<Test> getTests() {
        return this.fCache.asTestList(this.getDescription());
    }

    public Class<?> getTestClass() {
        return this.fNewTestClass;
    }

    @Override
    public Description getDescription() {
        Description description = this.fRunner.getDescription();
        return this.removeIgnored(description);
    }

    private Description removeIgnored(Description description) {
        if (this.isIgnored(description)) {
            return Description.EMPTY;
        }
        Description result = description.childlessCopy();
        for (Description each : description.getChildren()) {
            Description child = this.removeIgnored(each);
            if (child.isEmpty()) continue;
            result.addChild(child);
        }
        return result;
    }

    private boolean isIgnored(Description description) {
        return description.getAnnotation(Ignore.class) != null;
    }

    public String toString() {
        return this.fNewTestClass.getName();
    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        filter.apply(this.fRunner);
    }

    @Override
    public void sort(Sorter sorter) {
        sorter.apply(this.fRunner);
    }
}

