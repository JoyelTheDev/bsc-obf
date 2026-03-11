/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.propertyframework.impl;

import com.google.common.eventbus.EventBus;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.api.event.container.PropertyAddedEvent;
import org.mapleir.propertyframework.api.event.container.PropertyRemovedEvent;
import org.mapleir.propertyframework.util.PropertyHelper;

public class BasicPropertyDictionary
implements IPropertyDictionary {
    private final Map<String, IProperty<?>> map = new HashMap();
    private final EventBus bus;

    public BasicPropertyDictionary() {
        this(PropertyHelper.getFrameworkBus());
    }

    public BasicPropertyDictionary(EventBus bus) {
        this.bus = bus;
    }

    @Override
    public <T> IProperty<T> find(String key) {
        if (!this.map.containsKey(key)) {
            return null;
        }
        IProperty<?> prop = this.map.get(key);
        return prop;
    }

    @Override
    public <T> IProperty<T> find(Class<T> type, String key) {
        if (!this.map.containsKey(key)) {
            return null;
        }
        IProperty<?> prop = this.map.get(key);
        if (type == null || type.isAssignableFrom(prop.getType())) {
            return prop;
        }
        Class<?> rebasedType = PropertyHelper.rebasePrimitiveType(type);
        if (prop.getType().equals(rebasedType)) {
            return prop;
        }
        return null;
    }

    private void checkNotHeld(IProperty<?> p) {
        if (p.getDictionary() != null && p.getDictionary() != this) {
            throw new UnsupportedOperationException("Cannot link property to another dictionary");
        }
    }

    @Override
    public void put(String key, IProperty<?> property) {
        if (key == null || property == null) {
            throw new NullPointerException(String.format("Cannot map %s to %s", key, property));
        }
        this.checkNotHeld(property);
        IProperty<?> prev = this.map.put(key, property);
        if (prev != null) {
            this.bus.post(new PropertyRemovedEvent(prev, this, key));
        }
        this.bus.register(property);
        this.bus.post(new PropertyAddedEvent(property, this, key));
    }

    @Override
    public EventBus getContainerEventBus() {
        return this.bus;
    }

    @Override
    public Iterator<Map.Entry<String, IProperty<?>>> iterator() {
        return this.map.entrySet().iterator();
    }
}

