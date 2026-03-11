/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.map;

import org.mapleir.stdlib.collections.map.ValueCreator;

public class NullCreator<V>
implements ValueCreator<V> {
    @Override
    public V create() {
        return null;
    }
}

