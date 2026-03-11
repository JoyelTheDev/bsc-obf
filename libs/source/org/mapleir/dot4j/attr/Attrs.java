/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr;

import org.mapleir.dot4j.attr.MapAttrs;

public interface Attrs {
    public Attrs applyTo(MapAttrs var1);

    default public Attrs applyTo(Attrs attrs) {
        if (attrs instanceof MapAttrs) {
            return this.applyTo((MapAttrs)attrs);
        }
        throw new UnsupportedOperationException();
    }

    default public Object get(String key) {
        return this.applyTo(new MapAttrs()).get(key);
    }

    default public boolean isEmpty() {
        return this.applyTo(new MapAttrs()).isEmpty();
    }

    public static Attrs attr(String key, Object value) {
        return new MapAttrs().put(key, value);
    }

    public static Attrs attrs(Attrs ... attrss) {
        MapAttrs mapAttrs = new MapAttrs();
        for (Attrs attrs : attrss) {
            attrs.applyTo(mapAttrs);
        }
        return mapAttrs;
    }
}

