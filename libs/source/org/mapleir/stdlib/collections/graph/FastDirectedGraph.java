/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.graph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.propertyframework.api.IPropertyDictionary;
import org.mapleir.stdlib.collections.graph.FastGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.collections.graph.GraphUtils;

public abstract class FastDirectedGraph<N extends FastGraphVertex, E extends FastGraphEdge<N>>
implements FastGraph<N, E> {
    private final Map<N, Set<E>> map;
    private final Map<N, Set<E>> reverseMap;

    public FastDirectedGraph() {
        this.map = this.createMap();
        this.reverseMap = this.createMap();
    }

    public FastDirectedGraph(FastDirectedGraph<N, E> g) {
        this.map = this.createMap(g.map);
        this.reverseMap = this.createMap(g.reverseMap);
    }

    @Override
    public Set<N> vertices() {
        return Collections.unmodifiableSet(this.map.keySet());
    }

    @Override
    public boolean addVertex(N v) {
        boolean ret = false;
        if (!this.map.containsKey(v)) {
            this.map.put(v, this.createSet());
            ret = true;
        }
        if (!this.reverseMap.containsKey(v)) {
            if (!ret) {
                throw new IllegalStateException(v.toString());
            }
            this.reverseMap.put(v, this.createSet());
            ret = true;
        }
        return ret;
    }

    @Override
    public void removeVertex(N v) {
        for (FastGraphEdge e : this.map.remove(v)) {
            this.reverseMap.get(e.dst()).remove(e);
        }
        for (FastGraphEdge e : this.reverseMap.remove(v)) {
            this.map.get(e.src()).remove(e);
        }
    }

    @Override
    public boolean containsVertex(N v) {
        return this.map.containsKey(v);
    }

    public boolean containsReverseVertex(N v) {
        return this.reverseMap.containsKey(v);
    }

    @Override
    public void addEdge(E e) {
        Object src = e.src();
        this.addVertex(src);
        this.map.get(src).add(e);
        Object dst = e.dst();
        this.addVertex(dst);
        this.reverseMap.get(dst).add(e);
    }

    @Override
    public void removeEdge(E e) {
        Object dst;
        Object src = e.src();
        if (this.map.containsKey(src)) {
            this.map.get(src).remove(e);
        }
        if (this.reverseMap.containsKey(dst = e.dst())) {
            this.reverseMap.get(dst).remove(e);
        }
    }

    @Override
    public boolean containsEdge(E e) {
        Object src = e.src();
        return this.map.containsKey(src) && this.map.get(src).contains(e);
    }

    public boolean containsReverseEdge(E e) {
        Object dst = e.dst();
        return this.reverseMap.containsKey(dst) && this.reverseMap.get(dst).contains(e);
    }

    @Override
    public Set<E> getEdges(N b) {
        return Collections.unmodifiableSet(this.map.get(b));
    }

    public Stream<N> getSuccessors(N v) {
        return this.getEdges(v).stream().map(FastGraphEdge::dst);
    }

    public Set<E> getReverseEdges(N v) {
        return Collections.unmodifiableSet(this.reverseMap.get(v));
    }

    public Stream<N> getPredecessors(N v) {
        return this.getReverseEdges(v).stream().map(FastGraphEdge::src);
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public void replace(N old, N n) {
        FastGraphEdge newEdge;
        Set<E> succs = this.getEdges(old);
        Set<E> preds = this.getReverseEdges(old);
        this.addVertex(n);
        for (FastGraphEdge succ : new HashSet<E>(succs)) {
            newEdge = this.clone(succ, n, succ.dst());
            this.removeEdge(succ);
            this.addEdge(newEdge);
        }
        for (FastGraphEdge pred : new HashSet<E>(preds)) {
            newEdge = this.clone(pred, pred.src(), n);
            this.removeEdge(pred);
            this.addEdge(newEdge);
        }
        this.removeVertex(old);
    }

    @Override
    public void clear() {
        this.map.clear();
        this.reverseMap.clear();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("map {\n");
        for (Map.Entry<N, Set<E>> e : this.map.entrySet()) {
            sb.append("   ").append(e.getKey()).append("  ").append(e.getValue()).append("\n");
        }
        sb.append("}\n");
        sb.append("reverse {\n");
        for (Map.Entry<N, Set<E>> e : this.reverseMap.entrySet()) {
            sb.append("   ").append(e.getKey()).append("  ").append(e.getValue()).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public DotGraph makeDotGraph(IPropertyDictionary properties) {
        return GraphUtils.makeDotSkeleton(this).setDirected(true);
    }
}

