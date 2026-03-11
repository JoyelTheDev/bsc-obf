/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.propertyframework.impl;

import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.impl.BasicPropertyDictionary;

public class BasicSynchronisedPropertyDictionary
extends BasicPropertyDictionary {
    private final Object mapLock = new Object();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public <T> IProperty<T> find(String key) {
        Object object = this.mapLock;
        synchronized (object) {
            return super.find(key);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public <T> IProperty<T> find(Class<T> type, String key) {
        Object object = this.mapLock;
        synchronized (object) {
            return super.find(type, key);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void put(String key, IProperty<?> property) {
        Object object = this.mapLock;
        synchronized (object) {
            super.put(key, property);
        }
    }
}

