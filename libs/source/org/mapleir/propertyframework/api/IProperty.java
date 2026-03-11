/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.propertyframework.api;

import org.mapleir.propertyframework.api.IPropertyDictionary;

public interface IProperty<T> {
    public String getKey();

    public Class<T> getType();

    public IPropertyDictionary getDictionary();

    public void tryLinkDictionary(IPropertyDictionary var1);

    public T getValue();

    public void setValue(T var1);

    public IProperty<T> clone(IPropertyDictionary var1);
}

