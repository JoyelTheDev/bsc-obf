/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.theories.internal;

import java.util.ArrayList;
import java.util.List;
import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class EnumSupplier
extends ParameterSupplier {
    private Class<?> enumType;

    public EnumSupplier(Class<?> enumType) {
        this.enumType = enumType;
    }

    @Override
    public List<PotentialAssignment> getValueSources(ParameterSignature sig) {
        ?[] enumValues = this.enumType.getEnumConstants();
        ArrayList<PotentialAssignment> assignments = new ArrayList<PotentialAssignment>();
        for (Object value : enumValues) {
            assignments.add(PotentialAssignment.forValue(value.toString(), value));
        }
        return assignments;
    }
}

