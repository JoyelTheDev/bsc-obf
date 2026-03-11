/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.map;

import org.mapleir.stdlib.collections.map.KeyedValueCreator;

public interface ValueCreator<V>
extends KeyedValueCreator<Object, V> {
    public V create();

    @Override
    default public V create(Object o) {
        return this.create();
    }
}

