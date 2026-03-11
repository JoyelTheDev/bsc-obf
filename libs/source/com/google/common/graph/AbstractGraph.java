/*
 * Decompiled with CFR 0.152.
 */
package com.google.common.graph;

import com.google.common.annotations.Beta;
import com.google.common.graph.AbstractBaseGraph;
import com.google.common.graph.Graph;
import javax.annotation.Nullable;

@Beta
public abstract class AbstractGraph<N>
extends AbstractBaseGraph<N>
implements Graph<N> {
    @Override
    public final boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Graph)) {
            return false;
        }
        Graph other = (Graph)obj;
        return this.isDirected() == other.isDirected() && this.nodes().equals(other.nodes()) && this.edges().equals(other.edges());
    }

    @Override
    public final int hashCode() {
        return this.edges().hashCode();
    }

    public String toString() {
        String propertiesString = String.format("isDirected: %s, allowsSelfLoops: %s", this.isDirected(), this.allowsSelfLoops());
        return String.format("%s, nodes: %s, edges: %s", propertiesString, this.nodes(), this.edges());
    }
}

