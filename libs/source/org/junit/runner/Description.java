/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runner;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Description
implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Pattern METHOD_AND_CLASS_NAME_PATTERN = Pattern.compile("([\\s\\S]*)\\((.*)\\)");
    public static final Description EMPTY = new Description(null, "No Tests", new Annotation[0]);
    public static final Description TEST_MECHANISM = new Description(null, "Test mechanism", new Annotation[0]);
    private final Collection<Description> fChildren = new ConcurrentLinkedQueue<Description>();
    private final String fDisplayName;
    private final Serializable fUniqueId;
    private final Annotation[] fAnnotations;
    private volatile Class<?> fTestClass;

    public static Description createSuiteDescription(String name, Annotation ... annotations) {
        return new Description(null, name, annotations);
    }

    public static Description createSuiteDescription(String name, Serializable uniqueId, Annotation ... annotations) {
        return new Description(null, name, uniqueId, annotations);
    }

    public static Description createTestDescription(String className, String name, Annotation ... annotations) {
        return new Description(null, Description.formatDisplayName(name, className), annotations);
    }

    public static Description createTestDescription(Class<?> clazz, String name, Annotation ... annotations) {
        return new Description(clazz, Description.formatDisplayName(name, clazz.getName()), annotations);
    }

    public static Description createTestDescription(Class<?> clazz, String name) {
        return new Description(clazz, Description.formatDisplayName(name, clazz.getName()), new Annotation[0]);
    }

    public static Description createTestDescription(String className, String name, Serializable uniqueId) {
        return new Description(null, Description.formatDisplayName(name, className), uniqueId, new Annotation[0]);
    }

    private static String formatDisplayName(String name, String className) {
        return String.format("%s(%s)", name, className);
    }

    public static Description createSuiteDescription(Class<?> testClass) {
        return new Description(testClass, testClass.getName(), testClass.getAnnotations());
    }

    private Description(Class<?> clazz, String displayName, Annotation ... annotations) {
        this(clazz, displayName, (Serializable)((Object)displayName), annotations);
    }

    private Description(Class<?> testClass, String displayName, Serializable uniqueId, Annotation ... annotations) {
        if (displayName == null || displayName.length() == 0) {
            throw new IllegalArgumentException("The display name must not be empty.");
        }
        if (uniqueId == null) {
            throw new IllegalArgumentException("The unique id must not be null.");
        }
        this.fTestClass = testClass;
        this.fDisplayName = displayName;
        this.fUniqueId = uniqueId;
        this.fAnnotations = annotations;
    }

    public String getDisplayName() {
        return this.fDisplayName;
    }

    public void addChild(Description description) {
        this.fChildren.add(description);
    }

    public ArrayList<Description> getChildren() {
        return new ArrayList<Description>(this.fChildren);
    }

    public boolean isSuite() {
        return !this.isTest();
    }

    public boolean isTest() {
        return this.fChildren.isEmpty();
    }

    public int testCount() {
        if (this.isTest()) {
            return 1;
        }
        int result = 0;
        for (Description child : this.fChildren) {
            result += child.testCount();
        }
        return result;
    }

    public int hashCode() {
        return this.fUniqueId.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Description)) {
            return false;
        }
        Description d = (Description)obj;
        return this.fUniqueId.equals(d.fUniqueId);
    }

    public String toString() {
        return this.getDisplayName();
    }

    public boolean isEmpty() {
        return this.equals(EMPTY);
    }

    public Description childlessCopy() {
        return new Description(this.fTestClass, this.fDisplayName, this.fAnnotations);
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        for (Annotation each : this.fAnnotations) {
            if (!each.annotationType().equals(annotationType)) continue;
            return (T)((Annotation)annotationType.cast(each));
        }
        return null;
    }

    public Collection<Annotation> getAnnotations() {
        return Arrays.asList(this.fAnnotations);
    }

    public Class<?> getTestClass() {
        if (this.fTestClass != null) {
            return this.fTestClass;
        }
        String name = this.getClassName();
        if (name == null) {
            return null;
        }
        try {
            this.fTestClass = Class.forName(name, false, this.getClass().getClassLoader());
            return this.fTestClass;
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }

    public String getClassName() {
        return this.fTestClass != null ? this.fTestClass.getName() : this.methodAndClassNamePatternGroupOrDefault(2, this.toString());
    }

    public String getMethodName() {
        return this.methodAndClassNamePatternGroupOrDefault(1, null);
    }

    private String methodAndClassNamePatternGroupOrDefault(int group, String defaultString) {
        Matcher matcher = METHOD_AND_CLASS_NAME_PATTERN.matcher(this.toString());
        return matcher.matches() ? matcher.group(group) : defaultString;
    }
}

