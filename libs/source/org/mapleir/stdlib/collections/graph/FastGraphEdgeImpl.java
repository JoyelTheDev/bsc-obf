/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.graph;

import java.util.Objects;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public class FastGraphEdgeImpl<N extends FastGraphVertex>
implements FastGraphEdge<N> {
    protected final N src;
    protected final N dst;

    public FastGraphEdgeImpl(N src, N dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public N src() {
        return this.src;
    }

    @Override
    public N dst() {
        return this.dst;
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof FastGraphEdge)) {
            return false;
        }
        FastGraphEdge v = (FastGraphEdge)o;
        assert (v.src().getNumericId() == this.src.getNumericId() == (v.src() == this.src));
        assert (v.dst().getNumericId() == this.dst.getNumericId() == (v.dst() == this.dst));
        return v.src() == this.src && v.dst() == this.dst;
    }

    public int hashCode() {
        return Objects.hash(this.src, this.dst);
    }
}

