/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.theories;

import java.util.List;
import org.junit.experimental.theories.ParameterSignature;
import org.junit.experimental.theories.PotentialAssignment;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public abstract class ParameterSupplier {
    public abstract List<PotentialAssignment> getValueSources(ParameterSignature var1) throws Throwable;
}

