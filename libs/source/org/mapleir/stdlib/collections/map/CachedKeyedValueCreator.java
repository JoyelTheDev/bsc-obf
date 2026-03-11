/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.map;

import java.util.HashMap;
import java.util.Map;
import org.mapleir.stdlib.collections.map.KeyedValueCreator;
import org.mapleir.stdlib.collections.map.ValueCreator;

public abstract class CachedKeyedValueCreator<K, V>
implements KeyedValueCreator<K, V> {
    private final Map<K, V> map = this.makeMapImpl();

    protected Map<K, V> makeMapImpl() {
        return new HashMap();
    }

    public Map<K, V> getMap() {
        return this.map;
    }

    protected abstract V create0(K var1);

    @Override
    public V create(K k) {
        if (this.map.containsKey(k)) {
            return this.map.get(k);
        }
        V v = this.create0(k);
        this.map.put(k, v);
        return v;
    }

    public static class DelegatingCachedKeyedValueCreator<K, V>
    extends CachedKeyedValueCreator<K, V> {
        private final KeyedValueCreator<K, V> child;

        public DelegatingCachedKeyedValueCreator(ValueCreator<V> child) {
            this((KeyedValueCreator<K, V>)child);
        }

        public DelegatingCachedKeyedValueCreator(KeyedValueCreator<K, V> child) {
            this.child = child;
            if (child == null) {
                throw new NullPointerException();
            }
        }

        @Override
        protected V create0(K k) {
            return this.child.create(k);
        }
    }
}

