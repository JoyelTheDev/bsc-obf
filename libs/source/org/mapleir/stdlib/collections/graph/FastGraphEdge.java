/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.graph;

import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public interface FastGraphEdge<N extends FastGraphVertex> {
    public N src();

    public N dst();
}

