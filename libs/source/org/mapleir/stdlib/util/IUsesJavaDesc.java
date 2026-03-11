/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.util;

import org.mapleir.stdlib.util.IHasJavaDesc;
import org.mapleir.stdlib.util.JavaDesc;
import org.mapleir.stdlib.util.JavaDescUse;

public interface IUsesJavaDesc
extends IHasJavaDesc {
    public JavaDescUse.UseType getDataUseType();

    public JavaDesc getDataUseLocation();

    default public JavaDescUse getDataUse() {
        return new JavaDescUse(this.getJavaDesc(), this, this.getDataUseType());
    }
}

