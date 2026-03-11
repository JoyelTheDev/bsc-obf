/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.propertyframework.impl;

import java.util.Map;
import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.impl.BasicPropertyDictionary;

public class FixedStoreDictionary
extends BasicPropertyDictionary {
    public FixedStoreDictionary(IPropertyDictionary dict) {
        for (Map.Entry e : dict) {
            super.put((String)e.getKey(), ((IProperty)e.getValue()).clone(this));
        }
    }

    @Override
    public void put(String key, IProperty<?> property) {
        throw new UnsupportedOperationException("Cannot put; it's a fixedstore");
    }
}

