/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.propertyframework.api;

import com.google.common.eventbus.EventBus;
import java.util.Map;
import org.mapleir.propertyframework.api.IProperty;

public interface IPropertyDictionary
extends Iterable<Map.Entry<String, IProperty<?>>> {
    public <T> IProperty<T> find(String var1);

    public <T> IProperty<T> find(Class<T> var1, String var2);

    default public void put(IProperty<?> property) {
        if (property == null) {
            throw new NullPointerException("Null property");
        }
        this.put(property.getKey(), property);
    }

    public void put(String var1, IProperty<?> var2);

    public EventBus getContainerEventBus();
}

