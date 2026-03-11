/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.theories.internal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.internal.AllMembersSupplier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class SpecificDataPointsSupplier
extends AllMembersSupplier {
    public SpecificDataPointsSupplier(TestClass testClass) {
        super(testClass);
    }

    @Override
    protected Collection<Field> getSingleDataPointFields(ParameterSignature sig) {
        Collection<Field> fields = super.getSingleDataPointFields(sig);
        String requestedName = sig.getAnnotation(FromDataPoints.class).value();
        ArrayList<Field> fieldsWithMatchingNames = new ArrayList<Field>();
        for (Field field : fields) {
            String[] fieldNames = field.getAnnotation(DataPoint.class).value();
            if (!Arrays.asList(fieldNames).contains(requestedName)) continue;
            fieldsWithMatchingNames.add(field);
        }
        return fieldsWithMatchingNames;
    }

    @Override
    protected Collection<Field> getDataPointsFields(ParameterSignature sig) {
        Collection<Field> fields = super.getDataPointsFields(sig);
        String requestedName = sig.getAnnotation(FromDataPoints.class).value();
        ArrayList<Field> fieldsWithMatchingNames = new ArrayList<Field>();
        for (Field field : fields) {
            String[] fieldNames = field.getAnnotation(DataPoints.class).value();
            if (!Arrays.asList(fieldNames).contains(requestedName)) continue;
            fieldsWithMatchingNames.add(field);
        }
        return fieldsWithMatchingNames;
    }

    @Override
    protected Collection<FrameworkMethod> getSingleDataPointMethods(ParameterSignature sig) {
        Collection<FrameworkMethod> methods = super.getSingleDataPointMethods(sig);
        String requestedName = sig.getAnnotation(FromDataPoints.class).value();
        ArrayList<FrameworkMethod> methodsWithMatchingNames = new ArrayList<FrameworkMethod>();
        for (FrameworkMethod method : methods) {
            String[] methodNames = method.getAnnotation(DataPoint.class).value();
            if (!Arrays.asList(methodNames).contains(requestedName)) continue;
            methodsWithMatchingNames.add(method);
        }
        return methodsWithMatchingNames;
    }

    @Override
    protected Collection<FrameworkMethod> getDataPointsMethods(ParameterSignature sig) {
        Collection<FrameworkMethod> methods = super.getDataPointsMethods(sig);
        String requestedName = sig.getAnnotation(FromDataPoints.class).value();
        ArrayList<FrameworkMethod> methodsWithMatchingNames = new ArrayList<FrameworkMethod>();
        for (FrameworkMethod method : methods) {
            String[] methodNames = method.getAnnotation(DataPoints.class).value();
            if (!Arrays.asList(methodNames).contains(requestedName)) continue;
            methodsWithMatchingNames.add(method);
        }
        return methodsWithMatchingNames;
    }
}

