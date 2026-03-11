/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.mapleir.dot4j.attr.Attrs;

public class MapAttrs
implements Attrs,
Iterable<Map.Entry<String, Object>> {
    private final Map<String, Object> attributes = new HashMap<String, Object>();

    public MapAttrs put(String key, Object value) {
        if (value != null) {
            this.attributes.put(key, value);
        } else {
            this.attributes.remove(value);
        }
        return this;
    }

    public MapAttrs putAll(MapAttrs mapAttrs) {
        this.attributes.putAll(mapAttrs.attributes);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return this.attributes.isEmpty();
    }

    @Override
    public Object get(String key) {
        return this.attributes.get(key);
    }

    @Override
    public Attrs applyTo(MapAttrs mapAttrs) {
        mapAttrs.attributes.putAll(this.attributes);
        return mapAttrs;
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return this.attributes.entrySet().iterator();
    }

    public int hashCode() {
        return this.attributes.hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        MapAttrs other = (MapAttrs)obj;
        return this.attributes.equals(other.attributes);
    }

    public String toString() {
        return this.attributes.toString();
    }
}

