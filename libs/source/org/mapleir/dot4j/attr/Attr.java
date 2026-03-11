/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.MapAttrs;

public class Attr<T>
implements Attrs {
    protected final String key;
    protected final T value;

    public Attr(String key, T value) {
        this.key = key;
        this.value = value;
    }

    public <E> E key(String key) {
        return this.newInstance(key, this.value);
    }

    public <E> E value(T value) {
        return this.newInstance(this.key, value);
    }

    private <E> E newInstance(String key, T value) {
        try {
            ParameterizedType superclass = (ParameterizedType)this.getClass().getGenericSuperclass();
            Class type = (Class)superclass.getActualTypeArguments()[0];
            Constructor<? extends Object> cons = this.getClass().getDeclaredConstructor(String.class, type);
            cons.setAccessible(true);
            return (E)cons.newInstance(key, value);
        }
        catch (ReflectiveOperationException e) {
            throw new AssertionError((Object)e);
        }
    }

    @Override
    public Attrs applyTo(MapAttrs mapAttrs) {
        return mapAttrs.put(this.key, this.value);
    }
}

