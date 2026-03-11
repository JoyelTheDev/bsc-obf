/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.map;

import java.util.concurrent.atomic.AtomicInteger;
import org.mapleir.stdlib.collections.map.ValueCreator;

public class IntegerValueCreator
implements ValueCreator<AtomicInteger> {
    @Override
    public AtomicInteger create() {
        return new AtomicInteger();
    }
}

