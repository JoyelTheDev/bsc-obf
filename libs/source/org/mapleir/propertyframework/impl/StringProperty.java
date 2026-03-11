/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.propertyframework.impl;

import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.impl.DefaultValueProperty;

public class StringProperty
extends DefaultValueProperty<String> {
    public StringProperty(String key) {
        super(key, String.class, "");
    }

    public StringProperty(String key, String dflt) {
        super(key, String.class, dflt);
    }

    public StringProperty(String key, String dflt, String val) {
        super(key, String.class, dflt);
        this.setValue(val);
    }

    @Override
    public StringProperty clone(IPropertyDictionary newDict) {
        return new StringProperty(this.getKey(), (String)this.getDefault(), (String)this.getValue());
    }
}

