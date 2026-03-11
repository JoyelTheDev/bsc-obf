/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.propertyframework.impl;

import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.impl.DefaultValueProperty;

public class BooleanProperty
extends DefaultValueProperty<Boolean> {
    public BooleanProperty(String key) {
        this(key, false);
    }

    public BooleanProperty(String key, boolean dflt) {
        super(key, Boolean.class, dflt);
    }

    @Override
    public BooleanProperty clone(IPropertyDictionary newDict) {
        BooleanProperty p = new BooleanProperty(this.getKey(), (Boolean)this.getDefault());
        p.setValue((Boolean)this.getValue());
        return p;
    }
}

