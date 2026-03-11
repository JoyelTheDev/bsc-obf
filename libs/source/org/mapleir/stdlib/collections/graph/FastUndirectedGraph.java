/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.stdlib.collections.graph.FastGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.collections.graph.GraphUtils;

public abstract class FastUndirectedGraph<N extends FastGraphVertex, E extends FastGraphEdge<N>>
implements FastGraph<N, E> {
    private final Map<N, Set<E>> edgeSet;
    private final Map<E, E> sisterEdges;

    public FastUndirectedGraph() {
        this.edgeSet = this.createMap();
        this.sisterEdges = new HashMap<E, E>();
    }

    public FastUndirectedGraph(FastUndirectedGraph<N, E> g) {
        this.edgeSet = this.createMap(g.edgeSet);
        this.sisterEdges = new HashMap<E, E>(g.sisterEdges);
    }

    @Override
    public Set<N> vertices() {
        return new HashSet<N>(this.edgeSet.keySet());
    }

    @Override
    public boolean addVertex(N n) {
        if (this.edgeSet.containsKey(n)) {
            return false;
        }
        this.edgeSet.put(n, this.createSet());
        return true;
    }

    protected E getSisterEdge(E e) {
        if (this.sisterEdges.containsKey(e)) {
            return (E)((FastGraphEdge)this.sisterEdges.get(e));
        }
        throw new IllegalArgumentException(String.format("Edge is not mapped: %s", e));
    }

    @Override
    public void removeVertex(N n) {
        Set<E> edges = this.edgeSet.remove(n);
        if (edges != null && !edges.isEmpty()) {
            for (FastGraphEdge edge : edges) {
                FastGraphEdge sisterEdge = this.getSisterEdge(edge);
                FastGraphEdge e1 = (FastGraphEdge)this.sisterEdges.remove(edge);
                FastGraphEdge e2 = (FastGraphEdge)this.sisterEdges.remove(sisterEdge);
                assert (e1 == sisterEdge);
                assert (e2 == edge);
                this.edgeSet.get(edge.dst()).remove(sisterEdge);
            }
        }
    }

    @Override
    public boolean containsVertex(N n) {
        return this.edgeSet.containsKey(n);
    }

    @Override
    public void addEdge(E e) {
        if (this.containsEdge(e)) {
            return;
        }
        Object src = e.src();
        Object dst = e.dst();
        if (!this.containsVertex(src)) {
            this.addVertex(src);
        }
        if (!this.containsVertex(dst)) {
            this.addVertex(dst);
        }
        E sisterE = this.clone(e, dst, src);
        this.sisterEdges.put(e, sisterE);
        this.sisterEdges.put(sisterE, e);
        this.edgeSet.get(src).add(e);
        this.edgeSet.get(dst).add(sisterE);
    }

    @Override
    public void removeEdge(E e) {
        if (!this.containsEdge(e)) {
            return;
        }
        Object src = e.src();
        Object dst = e.dst();
        E sisterE = this.getSisterEdge(e);
        this.edgeSet.get(src).remove(e);
        this.edgeSet.get(dst).remove(sisterE);
    }

    @Override
    public boolean containsEdge(E e) {
        Object src = e.src();
        return this.edgeSet.containsKey(src) && this.edgeSet.get(src).contains(e);
    }

    @Override
    public Set<E> getEdges(N n) {
        return new HashSet(this.edgeSet.get(n));
    }

    @Override
    public int size() {
        return this.edgeSet.size();
    }

    @Override
    public void replace(N oldNode, N newNode) {
        HashSet edges = new HashSet(this.edgeSet.get(oldNode));
        for (FastGraphEdge e : edges) {
            assert (e.src() == oldNode);
            FastGraphEdge newEdge = this.clone(e, newNode, e.dst());
            this.removeEdge(e);
            this.addEdge(newEdge);
        }
        this.removeVertex(oldNode);
    }

    @Override
    public void clear() {
        this.edgeSet.clear();
        this.sisterEdges.clear();
    }

    @Override
    public DotGraph makeDotGraph(IPropertyDictionary properties) {
        HashSet addedEdges = new HashSet();
        return GraphUtils.makeDotSkeleton(this, null, (ourEdge, dotEdge) -> {
            if (addedEdges.contains(ourEdge)) {
                return false;
            }
            addedEdges.add(ourEdge);
            addedEdges.add(this.getSisterEdge(ourEdge));
            return true;
        });
    }
}

