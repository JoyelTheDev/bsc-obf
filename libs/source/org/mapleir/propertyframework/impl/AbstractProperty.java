/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.propertyframework.impl;

import com.google.common.eventbus.Subscribe;
import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.propertyframework.api.event.container.PropertyAddedEvent;
import org.mapleir.propertyframework.api.event.container.PropertyRemovedEvent;
import org.mapleir.propertyframework.api.event.update.PropertyValueChangedEvent;

public abstract class AbstractProperty<T>
implements IProperty<T> {
    private final String key;
    private final Class<T> type;
    private IPropertyDictionary container;
    private T value;

    public AbstractProperty(String key, Class<T> type) {
        this.key = key;
        this.type = type;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public Class<T> getType() {
        return this.type;
    }

    @Override
    public IPropertyDictionary getDictionary() {
        return this.container;
    }

    @Override
    public void tryLinkDictionary(IPropertyDictionary dict) {
        if (this.container == null) {
            this.container = dict;
            if (dict.find(this.getType(), this.getKey()) == null) {
                dict.put(this);
            } else {
                dict.getContainerEventBus().register(this);
            }
        }
    }

    @Override
    public T getValue() {
        IProperty<T> del = this.getDelegate();
        if (del != null) {
            return del.getValue();
        }
        if (this.value == null) {
            this.value = this.getDefault();
            if (this.container != null) {
                this.container.getContainerEventBus().post(new PropertyValueChangedEvent(this, null, this.value));
            }
        }
        return this.value;
    }

    @Override
    public void setValue(T t) {
        IProperty<T> del = this.getDelegate();
        if (del != null) {
            del.setValue(t);
        } else {
            T old = this.value;
            this.value = t;
            if (this.container != null) {
                this.container.getContainerEventBus().post(new PropertyValueChangedEvent(this, old, this.value));
            }
        }
    }

    protected IProperty<T> getDelegate() {
        if (this.container == null) {
            return null;
        }
        IProperty<T> prop = this.container.find(this.type, this.key);
        if (prop == null || prop == this) {
            return null;
        }
        return prop;
    }

    @Subscribe
    public void onPropertyAddedEvent(PropertyAddedEvent e) {
        if (!this.getKey().equals(e.getKey())) {
            return;
        }
        if (this.container == null) {
            this.container = e.getDictionary();
        }
    }

    @Subscribe
    public void onPropertyRemovedEvent(PropertyRemovedEvent e) {
        if (!this.getKey().equals(e.getKey())) {
            return;
        }
        if (this.container != null) {
            this.container.getContainerEventBus().unregister(this);
            this.container = null;
        }
    }

    public abstract T getDefault();
}

