/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.internal.MethodSorter;
import org.junit.runners.model.Annotatable;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMember;
import org.junit.runners.model.FrameworkMethod;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class TestClass
implements Annotatable {
    private static final FieldComparator FIELD_COMPARATOR = new FieldComparator();
    private static final MethodComparator METHOD_COMPARATOR = new MethodComparator();
    private final Class<?> clazz;
    private final Map<Class<? extends Annotation>, List<FrameworkMethod>> methodsForAnnotations;
    private final Map<Class<? extends Annotation>, List<FrameworkField>> fieldsForAnnotations;

    public TestClass(Class<?> clazz) {
        this.clazz = clazz;
        if (clazz != null && clazz.getConstructors().length > 1) {
            throw new IllegalArgumentException("Test class can only have one constructor");
        }
        LinkedHashMap methodsForAnnotations = new LinkedHashMap();
        LinkedHashMap fieldsForAnnotations = new LinkedHashMap();
        this.scanAnnotatedMembers(methodsForAnnotations, fieldsForAnnotations);
        this.methodsForAnnotations = TestClass.makeDeeplyUnmodifiable(methodsForAnnotations);
        this.fieldsForAnnotations = TestClass.makeDeeplyUnmodifiable(fieldsForAnnotations);
    }

    protected void scanAnnotatedMembers(Map<Class<? extends Annotation>, List<FrameworkMethod>> methodsForAnnotations, Map<Class<? extends Annotation>, List<FrameworkField>> fieldsForAnnotations) {
        for (Class<?> eachClass : TestClass.getSuperClasses(this.clazz)) {
            for (Method method : MethodSorter.getDeclaredMethods(eachClass)) {
                TestClass.addToAnnotationLists(new FrameworkMethod(method), methodsForAnnotations);
            }
            for (AccessibleObject accessibleObject : TestClass.getSortedDeclaredFields(eachClass)) {
                TestClass.addToAnnotationLists(new FrameworkField((Field)accessibleObject), fieldsForAnnotations);
            }
        }
    }

    private static Field[] getSortedDeclaredFields(Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        Arrays.sort(declaredFields, FIELD_COMPARATOR);
        return declaredFields;
    }

    protected static <T extends FrameworkMember<T>> void addToAnnotationLists(T member, Map<Class<? extends Annotation>, List<T>> map) {
        for (Annotation each : member.getAnnotations()) {
            Class<? extends Annotation> type = each.annotationType();
            List<T> members = TestClass.getAnnotatedMembers(map, type, true);
            if (((FrameworkMember)member).isShadowedBy(members)) {
                return;
            }
            if (TestClass.runsTopToBottom(type)) {
                members.add(0, member);
                continue;
            }
            members.add(member);
        }
    }

    private static <T extends FrameworkMember<T>> Map<Class<? extends Annotation>, List<T>> makeDeeplyUnmodifiable(Map<Class<? extends Annotation>, List<T>> source) {
        LinkedHashMap<Class<? extends Annotation>, List<T>> copy = new LinkedHashMap<Class<? extends Annotation>, List<T>>();
        for (Map.Entry<Class<Annotation>, List<T>> entry : source.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    public List<FrameworkMethod> getAnnotatedMethods() {
        List<FrameworkMethod> methods = this.collectValues(this.methodsForAnnotations);
        Collections.sort(methods, METHOD_COMPARATOR);
        return methods;
    }

    public List<FrameworkMethod> getAnnotatedMethods(Class<? extends Annotation> annotationClass) {
        return Collections.unmodifiableList(TestClass.getAnnotatedMembers(this.methodsForAnnotations, annotationClass, false));
    }

    public List<FrameworkField> getAnnotatedFields() {
        return this.collectValues(this.fieldsForAnnotations);
    }

    public List<FrameworkField> getAnnotatedFields(Class<? extends Annotation> annotationClass) {
        return Collections.unmodifiableList(TestClass.getAnnotatedMembers(this.fieldsForAnnotations, annotationClass, false));
    }

    private <T> List<T> collectValues(Map<?, List<T>> map) {
        LinkedHashSet<T> values = new LinkedHashSet<T>();
        for (List<T> additionalValues : map.values()) {
            values.addAll(additionalValues);
        }
        return new ArrayList(values);
    }

    private static <T> List<T> getAnnotatedMembers(Map<Class<? extends Annotation>, List<T>> map, Class<? extends Annotation> type, boolean fillIfAbsent) {
        List<T> members;
        if (!map.containsKey(type) && fillIfAbsent) {
            map.put(type, new ArrayList());
        }
        return (members = map.get(type)) == null ? Collections.emptyList() : members;
    }

    private static boolean runsTopToBottom(Class<? extends Annotation> annotation) {
        return annotation.equals(Before.class) || annotation.equals(BeforeClass.class);
    }

    private static List<Class<?>> getSuperClasses(Class<?> testClass) {
        ArrayList results = new ArrayList();
        for (Class<?> current = testClass; current != null; current = current.getSuperclass()) {
            results.add(current);
        }
        return results;
    }

    public Class<?> getJavaClass() {
        return this.clazz;
    }

    public String getName() {
        if (this.clazz == null) {
            return "null";
        }
        return this.clazz.getName();
    }

    public Constructor<?> getOnlyConstructor() {
        Constructor<?>[] constructors = this.clazz.getConstructors();
        Assert.assertEquals(1L, constructors.length);
        return constructors[0];
    }

    @Override
    public Annotation[] getAnnotations() {
        if (this.clazz == null) {
            return new Annotation[0];
        }
        return this.clazz.getAnnotations();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        if (this.clazz == null) {
            return null;
        }
        return this.clazz.getAnnotation(annotationType);
    }

    public <T> List<T> getAnnotatedFieldValues(Object test, Class<? extends Annotation> annotationClass, Class<T> valueClass) {
        ArrayList<T> results = new ArrayList<T>();
        for (FrameworkField each : this.getAnnotatedFields(annotationClass)) {
            try {
                Object fieldValue = each.get(test);
                if (!valueClass.isInstance(fieldValue)) continue;
                results.add(valueClass.cast(fieldValue));
            }
            catch (IllegalAccessException e) {
                throw new RuntimeException("How did getFields return a field we couldn't access?", e);
            }
        }
        return results;
    }

    public <T> List<T> getAnnotatedMethodValues(Object test, Class<? extends Annotation> annotationClass, Class<T> valueClass) {
        ArrayList<T> results = new ArrayList<T>();
        for (FrameworkMethod each : this.getAnnotatedMethods(annotationClass)) {
            try {
                if (!valueClass.isAssignableFrom(each.getReturnType())) continue;
                Object fieldValue = each.invokeExplosively(test, new Object[0]);
                results.add(valueClass.cast(fieldValue));
            }
            catch (Throwable e) {
                throw new RuntimeException("Exception in " + each.getName(), e);
            }
        }
        return results;
    }

    public boolean isPublic() {
        return Modifier.isPublic(this.clazz.getModifiers());
    }

    public boolean isANonStaticInnerClass() {
        return this.clazz.isMemberClass() && !Modifier.isStatic(this.clazz.getModifiers());
    }

    public int hashCode() {
        return this.clazz == null ? 0 : this.clazz.hashCode();
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
        TestClass other = (TestClass)obj;
        return this.clazz == other.clazz;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class MethodComparator
    implements Comparator<FrameworkMethod> {
        private MethodComparator() {
        }

        @Override
        public int compare(FrameworkMethod left, FrameworkMethod right) {
            return MethodSorter.NAME_ASCENDING.compare(left.getMethod(), right.getMethod());
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class FieldComparator
    implements Comparator<Field> {
        private FieldComparator() {
        }

        @Override
        public int compare(Field left, Field right) {
            return left.getName().compareTo(right.getName());
        }
    }
}

