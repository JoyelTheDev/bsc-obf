/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.propertyframework.impl;

import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.impl.DefaultValueProperty;

public class NumberProperty
extends DefaultValueProperty<Number> {
    public NumberProperty(String key) {
        super(key, Number.class, 0);
    }

    public NumberProperty(String key, Number dflt) {
        super(key, Number.class, dflt);
    }

    public float getFloat() {
        Number n = (Number)this.getValue();
        if (n != null) {
            return n.floatValue();
        }
        return 0.0f;
    }

    public double getDouble() {
        Number n = (Number)this.getValue();
        if (n != null) {
            return n.doubleValue();
        }
        return 0.0;
    }

    public int getInt() {
        Number n = (Number)this.getValue();
        if (n != null) {
            return n.intValue();
        }
        return 0;
    }

    public byte getByte() {
        Number n = (Number)this.getValue();
        if (n != null) {
            return n.byteValue();
        }
        return 0;
    }

    public short getShort() {
        Number n = (Number)this.getValue();
        if (n != null) {
            return n.shortValue();
        }
        return 0;
    }

    public long getLong() {
        Number n = (Number)this.getValue();
        if (n != null) {
            return n.longValue();
        }
        return 0L;
    }

    @Override
    public NumberProperty clone(IPropertyDictionary newDict) {
        NumberProperty n = new NumberProperty(this.getKey(), (Number)this.getDefault());
        n.setValue((Number)this.getValue());
        return n;
    }
}

