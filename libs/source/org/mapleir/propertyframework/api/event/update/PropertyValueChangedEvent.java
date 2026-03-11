/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.propertyframework.api.event.update;

import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.event.AbstractPropertyEvent;

public class PropertyValueChangedEvent
extends AbstractPropertyEvent {
    private final Object oldValue;
    private final Object newValue;

    public PropertyValueChangedEvent(IProperty<?> prop, Object oldValue, Object newValue) {
        super(prop);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public <T> T getOldValue() {
        return (T)this.oldValue;
    }

    public <T> T getNewValue() {
        return (T)this.newValue;
    }
}

