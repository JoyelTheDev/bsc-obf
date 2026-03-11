/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners.parameterized;

import org.junit.runner.Runner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.ParametersRunnerFactory;
import org.junit.runners.parameterized.TestWithParameters;

public class BlockJUnit4ClassRunnerWithParametersFactory
implements ParametersRunnerFactory {
    public Runner createRunnerForTestWithParameters(TestWithParameters test) throws InitializationError {
        return new BlockJUnit4ClassRunnerWithParameters(test);
    }
}

