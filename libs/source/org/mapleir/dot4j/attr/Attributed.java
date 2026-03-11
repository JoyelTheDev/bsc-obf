/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr;

import java.util.Map;
import org.mapleir.dot4j.attr.Attrs;

public interface Attributed<T>
extends Attrs,
Iterable<Map.Entry<String, Object>> {
    default public T with(String name, Object value) {
        return this.with(Attrs.attr(name, value));
    }

    default public T with(Attrs ... attrs) {
        return this.with(Attrs.attrs(attrs));
    }

    public T with(Attrs var1);
}

