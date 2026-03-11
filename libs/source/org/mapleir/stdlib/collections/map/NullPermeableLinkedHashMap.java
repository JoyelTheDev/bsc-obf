/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.map;

import java.util.LinkedHashMap;
import org.mapleir.stdlib.collections.map.NullCreator;
import org.mapleir.stdlib.collections.map.ValueCreator;

public class NullPermeableLinkedHashMap<K, V>
extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = 1L;
    private final ValueCreator<V> creator;

    public NullPermeableLinkedHashMap(ValueCreator<V> creator) {
        this.creator = creator;
    }

    public NullPermeableLinkedHashMap() {
        this(new NullCreator());
    }

    public V getNotNull(K k) {
        return (V)this.computeIfAbsent(k, this.creator::create);
    }
}

