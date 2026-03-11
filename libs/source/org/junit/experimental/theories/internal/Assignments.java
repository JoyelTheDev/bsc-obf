/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.theories.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.ParametersSuppliedBy;
import org.junit.experimental.theories.PotentialAssignment;
import org.junit.experimental.theories.internal.AllMembersSupplier;
import org.junit.experimental.theories.internal.BooleanSupplier;
import org.junit.experimental.theories.internal.EnumSupplier;
import org.junit.runners.model.TestClass;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Assignments {
    private final List<PotentialAssignment> assigned;
    private final List<ParameterSignature> unassigned;
    private final TestClass clazz;

    private Assignments(List<PotentialAssignment> assigned, List<ParameterSignature> unassigned, TestClass clazz) {
        this.unassigned = unassigned;
        this.assigned = assigned;
        this.clazz = clazz;
    }

    public static Assignments allUnassigned(Method testMethod, TestClass testClass) {
        List<ParameterSignature> signatures = ParameterSignature.signatures(testClass.getOnlyConstructor());
        signatures.addAll(ParameterSignature.signatures(testMethod));
        return new Assignments(new ArrayList<PotentialAssignment>(), signatures, testClass);
    }

    public boolean isComplete() {
        return this.unassigned.size() == 0;
    }

    public ParameterSignature nextUnassigned() {
        return this.unassigned.get(0);
    }

    public Assignments assignNext(PotentialAssignment source) {
        ArrayList<PotentialAssignment> assigned = new ArrayList<PotentialAssignment>(this.assigned);
        assigned.add(source);
        return new Assignments(assigned, this.unassigned.subList(1, this.unassigned.size()), this.clazz);
    }

    public Object[] getActualValues(int start, int stop) throws PotentialAssignment.CouldNotGenerateValueException {
        Object[] values = new Object[stop - start];
        for (int i = start; i < stop; ++i) {
            values[i - start] = this.assigned.get(i).getValue();
        }
        return values;
    }

    public List<PotentialAssignment> potentialsForNextUnassigned() throws Throwable {
        ParameterSignature unassigned = this.nextUnassigned();
        List<PotentialAssignment> assignments = this.getSupplier(unassigned).getValueSources(unassigned);
        if (assignments.size() == 0) {
            assignments = this.generateAssignmentsFromTypeAlone(unassigned);
        }
        return assignments;
    }

    private List<PotentialAssignment> generateAssignmentsFromTypeAlone(ParameterSignature unassigned) {
        Class<?> paramType = unassigned.getType();
        if (paramType.isEnum()) {
            return new EnumSupplier(paramType).getValueSources(unassigned);
        }
        if (paramType.equals(Boolean.class) || paramType.equals(Boolean.TYPE)) {
            return new BooleanSupplier().getValueSources(unassigned);
        }
        return Collections.emptyList();
    }

    private ParameterSupplier getSupplier(ParameterSignature unassigned) throws Exception {
        ParametersSuppliedBy annotation = unassigned.findDeepAnnotation(ParametersSuppliedBy.class);
        if (annotation != null) {
            return this.buildParameterSupplierFromClass(annotation.value());
        }
        return new AllMembersSupplier(this.clazz);
    }

    private ParameterSupplier buildParameterSupplierFromClass(Class<? extends ParameterSupplier> cls) throws Exception {
        Constructor<?>[] supplierConstructors;
        for (Constructor<?> constructor : supplierConstructors = cls.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length != 1 || !parameterTypes[0].equals(TestClass.class)) continue;
            return (ParameterSupplier)constructor.newInstance(this.clazz);
        }
        return cls.newInstance();
    }

    public Object[] getConstructorArguments() throws PotentialAssignment.CouldNotGenerateValueException {
        return this.getActualValues(0, this.getConstructorParameterCount());
    }

    public Object[] getMethodArguments() throws PotentialAssignment.CouldNotGenerateValueException {
        return this.getActualValues(this.getConstructorParameterCount(), this.assigned.size());
    }

    public Object[] getAllArguments() throws PotentialAssignment.CouldNotGenerateValueException {
        return this.getActualValues(0, this.assigned.size());
    }

    private int getConstructorParameterCount() {
        List<ParameterSignature> signatures = ParameterSignature.signatures(this.clazz.getOnlyConstructor());
        int constructorParameterCount = signatures.size();
        return constructorParameterCount;
    }

    public Object[] getArgumentStrings(boolean nullsOk) throws PotentialAssignment.CouldNotGenerateValueException {
        Object[] values = new Object[this.assigned.size()];
        for (int i = 0; i < values.length; ++i) {
            values[i] = this.assigned.get(i).getDescription();
        }
        return values;
    }
}

