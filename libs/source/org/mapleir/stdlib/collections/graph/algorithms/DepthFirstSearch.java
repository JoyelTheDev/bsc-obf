/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.graph.algorithms;

import java.util.List;

public interface DepthFirstSearch<N> {
    public List<N> getPreOrder();

    public List<N> getPostOrder();

    public List<N> getTopoOrder();
}

