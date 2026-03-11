/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.propertyframework.impl;

import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.impl.AbstractProperty;

public class DefaultValueProperty<T>
extends AbstractProperty<T> {
    private final T dflt;

    public DefaultValueProperty(String key, Class<T> type, T dflt) {
        super(key, type);
        this.dflt = dflt;
    }

    @Override
    public T getDefault() {
        return this.dflt;
    }

    @Override
    public DefaultValueProperty<T> clone(IPropertyDictionary newDict) {
        DefaultValueProperty p = new DefaultValueProperty(this.getKey(), this.getType(), this.dflt);
        p.setValue(this.getValue());
        return p;
    }
}

