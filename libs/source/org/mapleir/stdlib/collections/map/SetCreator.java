/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.map;

import java.util.HashSet;
import java.util.Set;
import org.mapleir.stdlib.collections.map.ValueCreator;

public class SetCreator<T>
implements ValueCreator<Set<T>> {
    private static final SetCreator<?> INSTANCE = new SetCreator();

    public static <T> SetCreator<T> getInstance() {
        return INSTANCE;
    }

    @Override
    public Set<T> create() {
        return new HashSet();
    }
}

