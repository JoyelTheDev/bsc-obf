/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.graph.algorithms;

import java.util.List;
import org.mapleir.stdlib.collections.graph.FastDirectedGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.collections.graph.algorithms.DepthFirstSearch;
import org.mapleir.stdlib.collections.graph.algorithms.ExtendedDfs;

@Deprecated
public class SimpleDfs<N extends FastGraphVertex>
implements DepthFirstSearch<N> {
    public static final int REVERSE = 16;
    public static final int PRE = 4;
    public static final int POST = 8;
    public static final int TOPO = 64;
    private ExtendedDfs<N> impl;

    public SimpleDfs(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry, int flags) {
        this.impl = new ExtendedDfs<N>(graph, flags).run(entry);
    }

    public static <N extends FastGraphVertex> List<N> preorder(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry) {
        return SimpleDfs.preorder(graph, entry, false);
    }

    public static <N extends FastGraphVertex> List<N> preorder(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry, boolean reverse) {
        return new SimpleDfs<N>(graph, entry, 4 | (reverse ? 16 : 0)).getPreOrder();
    }

    public static <N extends FastGraphVertex> List<N> postorder(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry) {
        return SimpleDfs.postorder(graph, entry, false);
    }

    public static <N extends FastGraphVertex> List<N> postorder(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry, boolean reverse) {
        return new SimpleDfs<N>(graph, entry, 8 | (reverse ? 16 : 0)).getPostOrder();
    }

    public static <N extends FastGraphVertex> List<N> topoorder(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry) {
        return SimpleDfs.topoorder(graph, entry, false);
    }

    public static <N extends FastGraphVertex> List<N> topoorder(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, N entry, boolean reverse) {
        return new SimpleDfs<N>(graph, entry, 0x48 | (reverse ? 16 : 0)).getTopoOrder();
    }

    @Override
    public List<N> getPreOrder() {
        return this.impl.getPreOrder();
    }

    @Override
    public List<N> getPostOrder() {
        return this.impl.getPostOrder();
    }

    @Override
    public List<N> getTopoOrder() {
        return this.impl.getTopoOrder();
    }
}

