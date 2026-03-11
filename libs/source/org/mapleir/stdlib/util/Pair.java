/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.util;

import java.util.Objects;

public class Pair<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return this.key;
    }

    public V getValue() {
        return this.value;
    }

    public String toString() {
        return "(" + this.key + ", " + this.value + ")";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Pair pair = (Pair)o;
        return Objects.equals(this.key, pair.key) && Objects.equals(this.value, pair.value);
    }

    public int hashCode() {
        return Objects.hash(this.key, this.value);
    }
}

