/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.theories.suppliers;

import java.util.ArrayList;
import java.util.List;
import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.ParameterSupplier;
import org.junit.experimental.theories.PotentialAssignment;
import org.junit.experimental.theories.suppliers.TestedOn;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class TestedOnSupplier
extends ParameterSupplier {
    @Override
    public List<PotentialAssignment> getValueSources(ParameterSignature sig) {
        int[] ints;
        ArrayList<PotentialAssignment> list = new ArrayList<PotentialAssignment>();
        TestedOn testedOn = sig.getAnnotation(TestedOn.class);
        for (int i : ints = testedOn.ints()) {
            list.add(PotentialAssignment.forValue("ints", i));
        }
        return list;
    }
}

