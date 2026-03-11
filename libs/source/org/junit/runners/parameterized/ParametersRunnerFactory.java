/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners.parameterized;

import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.parameterized.TestWithParameters;

public interface ParametersRunnerFactory {
    public Runner createRunnerForTestWithParameters(TestWithParameters var1) throws InitializationError;
}

