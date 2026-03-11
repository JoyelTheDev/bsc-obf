/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.theories.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.junit.Assume;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class AllMembersSupplier
extends ParameterSupplier {
    private final TestClass clazz;

    public AllMembersSupplier(TestClass type) {
        this.clazz = type;
    }

    @Override
    public List<PotentialAssignment> getValueSources(ParameterSignature sig) throws Throwable {
        ArrayList<PotentialAssignment> list = new ArrayList<PotentialAssignment>();
        this.addSinglePointFields(sig, list);
        this.addMultiPointFields(sig, list);
        this.addSinglePointMethods(sig, list);
        this.addMultiPointMethods(sig, list);
        return list;
    }

    private void addMultiPointMethods(ParameterSignature sig, List<PotentialAssignment> list) throws Throwable {
        for (FrameworkMethod dataPointsMethod : this.getDataPointsMethods(sig)) {
            Class<?> returnType = dataPointsMethod.getReturnType();
            if ((!returnType.isArray() || !sig.canPotentiallyAcceptType(returnType.getComponentType())) && !Iterable.class.isAssignableFrom(returnType)) continue;
            try {
                this.addDataPointsValues(returnType, sig, dataPointsMethod.getName(), list, dataPointsMethod.invokeExplosively(null, new Object[0]));
            }
            catch (Throwable throwable) {
                DataPoints annotation = dataPointsMethod.getAnnotation(DataPoints.class);
                if (annotation != null && AllMembersSupplier.isAssignableToAnyOf(annotation.ignoredExceptions(), throwable)) {
                    return;
                }
                throw throwable;
            }
        }
    }

    private void addSinglePointMethods(ParameterSignature sig, List<PotentialAssignment> list) {
        for (FrameworkMethod dataPointMethod : this.getSingleDataPointMethods(sig)) {
            if (!sig.canAcceptType(dataPointMethod.getType())) continue;
            list.add(new MethodParameterValue(dataPointMethod));
        }
    }

    private void addMultiPointFields(ParameterSignature sig, List<PotentialAssignment> list) {
        for (Field field : this.getDataPointsFields(sig)) {
            Class<?> type = field.getType();
            this.addDataPointsValues(type, sig, field.getName(), list, this.getStaticFieldValue(field));
        }
    }

    private void addSinglePointFields(ParameterSignature sig, List<PotentialAssignment> list) {
        for (Field field : this.getSingleDataPointFields(sig)) {
            Object value = this.getStaticFieldValue(field);
            if (!sig.canAcceptValue(value)) continue;
            list.add(PotentialAssignment.forValue(field.getName(), value));
        }
    }

    private void addDataPointsValues(Class<?> type, ParameterSignature sig, String name, List<PotentialAssignment> list, Object value) {
        if (type.isArray()) {
            this.addArrayValues(sig, name, list, value);
        } else if (Iterable.class.isAssignableFrom(type)) {
            this.addIterableValues(sig, name, list, (Iterable)value);
        }
    }

    private void addArrayValues(ParameterSignature sig, String name, List<PotentialAssignment> list, Object array) {
        for (int i = 0; i < Array.getLength(array); ++i) {
            Object value = Array.get(array, i);
            if (!sig.canAcceptValue(value)) continue;
            list.add(PotentialAssignment.forValue(name + "[" + i + "]", value));
        }
    }

    private void addIterableValues(ParameterSignature sig, String name, List<PotentialAssignment> list, Iterable<?> iterable) {
        Iterator<?> iterator = iterable.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            Object value = iterator.next();
            if (sig.canAcceptValue(value)) {
                list.add(PotentialAssignment.forValue(name + "[" + i + "]", value));
            }
            ++i;
        }
    }

    private Object getStaticFieldValue(Field field) {
        try {
            return field.get(null);
        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException("unexpected: field from getClass doesn't exist on object");
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException("unexpected: getFields returned an inaccessible field");
        }
    }

    private static boolean isAssignableToAnyOf(Class<?>[] typeArray, Object target) {
        for (Class<? extends Object> clazz : typeArray) {
            if (!clazz.isAssignableFrom(target.getClass())) continue;
            return true;
        }
        return false;
    }

    protected Collection<FrameworkMethod> getDataPointsMethods(ParameterSignature sig) {
        return this.clazz.getAnnotatedMethods(DataPoints.class);
    }

    protected Collection<Field> getSingleDataPointFields(ParameterSignature sig) {
        List<FrameworkField> fields = this.clazz.getAnnotatedFields(DataPoint.class);
        ArrayList<Field> validFields = new ArrayList<Field>();
        for (FrameworkField frameworkField : fields) {
            validFields.add(frameworkField.getField());
        }
        return validFields;
    }

    protected Collection<Field> getDataPointsFields(ParameterSignature sig) {
        List<FrameworkField> fields = this.clazz.getAnnotatedFields(DataPoints.class);
        ArrayList<Field> validFields = new ArrayList<Field>();
        for (FrameworkField frameworkField : fields) {
            validFields.add(frameworkField.getField());
        }
        return validFields;
    }

    protected Collection<FrameworkMethod> getSingleDataPointMethods(ParameterSignature sig) {
        return this.clazz.getAnnotatedMethods(DataPoint.class);
    }

    static class MethodParameterValue
    extends PotentialAssignment {
        private final FrameworkMethod method;

        private MethodParameterValue(FrameworkMethod dataPointMethod) {
            this.method = dataPointMethod;
        }

        public Object getValue() throws PotentialAssignment.CouldNotGenerateValueException {
            try {
                return this.method.invokeExplosively(null, new Object[0]);
            }
            catch (IllegalArgumentException e) {
                throw new RuntimeException("unexpected: argument length is checked");
            }
            catch (IllegalAccessException e) {
                throw new RuntimeException("unexpected: getMethods returned an inaccessible method");
            }
            catch (Throwable throwable) {
                DataPoint annotation = this.method.getAnnotation(DataPoint.class);
                Assume.assumeTrue(annotation == null || !AllMembersSupplier.isAssignableToAnyOf(annotation.ignoredExceptions(), throwable));
                throw new PotentialAssignment.CouldNotGenerateValueException(throwable);
            }
        }

        public String getDescription() throws PotentialAssignment.CouldNotGenerateValueException {
            return this.method.getName();
        }
    }
}

