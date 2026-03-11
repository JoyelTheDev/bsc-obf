/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.propertyframework.api.event;

import org.mapleir.propertyframework.api.IProperty;
import org.mapleir.propertyframework.api.event.IPropertyEvent;

public abstract class AbstractPropertyEvent
implements IPropertyEvent {
    private final IProperty<?> prop;

    public AbstractPropertyEvent(IProperty<?> prop) {
        this.prop = prop;
    }

    @Override
    public IProperty<?> getProperty() {
        return this.prop;
    }
}

