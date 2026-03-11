/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.graph.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.mapleir.stdlib.collections.graph.FastDirectedGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.collections.graph.algorithms.ExtendedDfs;

public class TarjanSCC<N extends FastGraphVertex> {
    protected final FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph;
    protected final Map<N, Integer> index;
    protected final Map<N, Integer> low;
    protected final LinkedList<N> stack;
    protected final List<List<N>> comps;
    protected int cur;

    public TarjanSCC(FastDirectedGraph<N, ? extends FastGraphEdge<N>> graph) {
        this.graph = graph;
        this.index = new HashMap<N, Integer>();
        this.low = new HashMap<N, Integer>();
        this.stack = new LinkedList();
        this.comps = new ArrayList<List<N>>();
    }

    public int low(N n) {
        return this.low.getOrDefault(n, -1);
    }

    public int index(N n) {
        return this.index.getOrDefault(n, -1);
    }

    public List<List<N>> getComponents() {
        return this.comps;
    }

    public void search(N n) {
        this.index.put(n, this.cur);
        this.low.put(n, this.cur);
        ++this.cur;
        this.stack.push(n);
        for (FastGraphEdge<N> e : this.filter(this.getEdges(n))) {
            N s = this.dst(e);
            if (this.low.containsKey(s)) {
                if (this.index.get(s) >= this.index.get(n) || !this.stack.contains(s)) continue;
                this.low.put(n, Math.min(this.low.get(n), this.index.get(s)));
                continue;
            }
            this.search(s);
            this.low.put(n, Math.min(this.low.get(n), this.low.get(s)));
        }
        if (Objects.equals(this.low.get(n), this.index.get(n))) {
            HashSet<FastGraphVertex> c = new HashSet<FastGraphVertex>();
            FastGraphVertex w = null;
            do {
                w = (FastGraphVertex)this.stack.pop();
                c.add(w);
            } while (w != n);
            this.comps.add(0, this.formComponent(c, n));
        }
    }

    protected N dst(FastGraphEdge<N> e) {
        return e.dst();
    }

    protected List<N> formComponent(Set<N> s, N found) {
        ExtendedDfs<N> dfs = new ExtendedDfs<N>(this.graph, 64).setMask(s).run(found);
        return dfs.getTopoOrder();
    }

    protected Set<? extends FastGraphEdge<N>> getEdges(N n) {
        return this.graph.getEdges(n);
    }

    protected Iterable<? extends FastGraphEdge<N>> filter(Set<? extends FastGraphEdge<N>> edges) {
        return edges;
    }
}

