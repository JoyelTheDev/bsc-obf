/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.model;

@FunctionalInterface
public interface ThrowingFunction<T, R> {
    public R apply(T var1) throws Exception;
}

