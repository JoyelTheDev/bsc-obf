/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners.parameterized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.runners.model.TestClass;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class TestWithParameters {
    private final String name;
    private final TestClass testClass;
    private final List<Object> parameters;

    public TestWithParameters(String name, TestClass testClass, List<Object> parameters) {
        TestWithParameters.notNull(name, "The name is missing.");
        TestWithParameters.notNull(testClass, "The test class is missing.");
        TestWithParameters.notNull(parameters, "The parameters are missing.");
        this.name = name;
        this.testClass = testClass;
        this.parameters = Collections.unmodifiableList(new ArrayList<Object>(parameters));
    }

    public String getName() {
        return this.name;
    }

    public TestClass getTestClass() {
        return this.testClass;
    }

    public List<Object> getParameters() {
        return this.parameters;
    }

    public int hashCode() {
        int prime = 14747;
        int result = prime + this.name.hashCode();
        result = prime * result + this.testClass.hashCode();
        return prime * result + ((Object)this.parameters).hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        TestWithParameters other = (TestWithParameters)obj;
        return this.name.equals(other.name) && ((Object)this.parameters).equals(other.parameters) && this.testClass.equals(other.testClass);
    }

    public String toString() {
        return this.testClass.getName() + " '" + this.name + "' with parameters " + this.parameters;
    }

    private static void notNull(Object value, String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
    }
}

