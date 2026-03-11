/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.graph.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mapleir.stdlib.collections.graph.FastDirectedGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.collections.graph.algorithms.DepthFirstSearch;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;

public class ExtendedDfs<N extends FastGraphVertex>
implements DepthFirstSearch<N> {
    public static final int WHITE = 0;
    public static final int GREY = 1;
    public static final int BLACK = 2;
    public static final int TREE = 0;
    public static final int BACK = 1;
    public static final int FOR_CROSS = 2;
    public static final int EDGES = 1;
    public static final int PARENTS = 2;
    public static final int PRE = 4;
    public static final int POST = 8;
    public static final int REVERSE = 16;
    public static final int COLOUR_VISITOR = 32;
    public static final int TOPO = 64;
    private final int opt;
    private Collection<N> mask;
    private final FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph;
    private final NullPermeableHashMap<N, Integer> colours;
    private final Map<Integer, Set<FastGraphEdge<N>>> edges;
    private final Map<N, N> parents;
    private final List<N> preorder;
    private final List<N> postorder;
    private List<N> topoorder;

    public ExtendedDfs(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph, int opt) {
        this.opt = opt;
        this.graph = graph;
        this.colours = new NullPermeableHashMap<FastGraphVertex, Integer>(k -> {
            if (this.opt(32)) {
                this.coloured(k, 0);
            }
            return 0;
        });
        this.parents = this.opt(2) ? new HashMap() : null;
        this.preorder = this.opt(4) ? new ArrayList() : null;
        List<Object> list = this.postorder = this.opt(8) || this.opt(64) ? new ArrayList<N>() : null;
        if (this.opt(1)) {
            this.edges = new HashMap<Integer, Set<FastGraphEdge<N>>>();
            this.edges.put(0, new HashSet());
            this.edges.put(1, new HashSet());
            this.edges.put(2, new HashSet());
        } else {
            this.edges = null;
        }
        this.mask = null;
    }

    public ExtendedDfs<N> run(N entry) {
        this.dfs(null, entry);
        if (this.opt(64)) {
            this.topoorder = this.opt(8) ? new ArrayList<N>(this.postorder) : this.postorder;
            Collections.reverse(this.topoorder);
        }
        return this;
    }

    public ExtendedDfs<N> setMask(Collection<N> mask) {
        this.mask = mask;
        return this;
    }

    public void clearMask() {
        this.mask = null;
    }

    public int getColour(N b) {
        return (Integer)this.colours.get(b);
    }

    public Map<N, N> getParents() {
        return this.parents;
    }

    public N getParent(N b) {
        return (N)((FastGraphVertex)this.parents.get(b));
    }

    public Set<FastGraphEdge<N>> getEdges(int type) {
        return this.edges.get(type);
    }

    private boolean opt(int i) {
        return (this.opt & i) != 0;
    }

    protected void dfs(N par, N b) {
        boolean cvisit = this.opt(32);
        boolean reverse = this.opt(16);
        if (this.opt(4)) {
            this.preorder.add(b);
        }
        this.colours.put(b, 1);
        if (cvisit) {
            this.coloured(b, 1);
        }
        for (FastGraphEdge<N> sE : this.order(reverse ? this.graph.getReverseEdges(b) : this.graph.getEdges(b))) {
            N s;
            N n = s = reverse ? sE.src() : sE.dst();
            if (this.mask != null && !this.mask.contains(s)) continue;
            if (this.opt(1)) {
                this.edges.get(this.colours.getNonNull(s)).add(sE);
            }
            if (this.colours.getNonNull(s) != 0) continue;
            if (this.opt(2)) {
                this.parents.put(s, b);
            }
            this.dfs(b, s);
        }
        if (this.opt(8) || this.opt(64)) {
            this.postorder.add(b);
        }
        this.colours.put(b, 2);
        if (cvisit) {
            this.coloured(b, 2);
        }
    }

    protected void coloured(N n, int c) {
    }

    protected Iterable<? extends FastGraphEdge<N>> order(Set<? extends FastGraphEdge<N>> edges) {
        return edges;
    }

    @Override
    public List<N> getPreOrder() {
        return this.preorder;
    }

    @Override
    public List<N> getPostOrder() {
        return this.postorder;
    }

    @Override
    public List<N> getTopoOrder() {
        return this.topoorder;
    }
}

