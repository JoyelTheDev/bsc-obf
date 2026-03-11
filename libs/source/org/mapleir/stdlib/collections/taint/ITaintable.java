/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.taint;

public interface ITaintable {
    public boolean isTainted();

    public boolean union(ITaintable var1);
}

