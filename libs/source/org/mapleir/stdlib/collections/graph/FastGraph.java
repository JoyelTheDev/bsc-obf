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
import org.mapleir.propertyframework.util.PropertyHelper;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public interface FastGraph<N extends FastGraphVertex, E extends FastGraphEdge<N>> {
    public Set<N> vertices();

    public boolean addVertex(N var1);

    public void removeVertex(N var1);

    public boolean containsVertex(N var1);

    public void addEdge(E var1);

    public void removeEdge(E var1);

    public boolean containsEdge(E var1);

    public Set<E> getEdges(N var1);

    public int size();

    default public E clone(E edge, N newSrc, N newDst) {
        throw new UnsupportedOperationException();
    }

    public void replace(N var1, N var2);

    public void clear();

    default public FastGraph<N, E> copy() {
        throw new UnsupportedOperationException();
    }

    default public Map<N, Set<E>> createMap() {
        return new HashMap();
    }

    default public Map<N, Set<E>> createMap(Map<N, Set<E>> map) {
        Map<N, Set<E>> map2 = this.createMap();
        for (Map.Entry<N, Set<E>> e : map.entrySet()) {
            map2.put((FastGraphVertex)e.getKey(), this.createSet(e.getValue()));
        }
        return map2;
    }

    default public Set<E> createSet() {
        return new HashSet();
    }

    default public Set<E> createSet(Set<E> set) {
        Set<E> newSet = this.createSet();
        newSet.addAll(set);
        return newSet;
    }

    default public DotGraph makeDotGraph() {
        return this.makeDotGraph(PropertyHelper.getImmutableDictionary());
    }

    public DotGraph makeDotGraph(IPropertyDictionary var1);
}

