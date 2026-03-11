/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr;

import java.util.Iterator;
import java.util.Map;
import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.MapAttrs;

public class SimpleAttributed<E>
implements Attributed<E> {
    private final E delegate;
    private final MapAttrs attrs;

    public SimpleAttributed(E delegate) {
        this.delegate = delegate;
        this.attrs = new MapAttrs();
    }

    public SimpleAttributed(E delegate, Attrs attrs) {
        this.delegate = delegate;
        this.attrs = new MapAttrs();
        if (attrs != null) {
            attrs.applyTo(this.attrs);
        }
    }

    @Override
    public Attrs applyTo(MapAttrs mapAttrs) {
        return mapAttrs.putAll(this.attrs);
    }

    @Override
    public E with(Attrs attrs) {
        attrs.applyTo(this.attrs);
        return this.delegate;
    }

    @Override
    public Object get(String key) {
        return this.attrs.get(key);
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
        SimpleAttributed other = (SimpleAttributed)obj;
        if (this.attrs == null ? other.attrs != null : !this.attrs.equals(other.attrs)) {
            return false;
        }
        return !(this.delegate == null ? other.delegate != null : !this.delegate.equals(other.delegate));
    }

    public int hashCode() {
        return this.attrs.hashCode();
    }

    public String toString() {
        return this.attrs.toString();
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return this.attrs.iterator();
    }
}

