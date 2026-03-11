/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.util;

import org.mapleir.stdlib.util.JavaDesc;

public interface IHasJavaDesc {
    public String getOwner();

    public String getName();

    public String getDesc();

    public JavaDesc.DescType getDescType();

    default public JavaDesc getJavaDesc() {
        return new JavaDesc(this.getOwner(), this.getName(), this.getDesc(), this.getDescType());
    }
}

