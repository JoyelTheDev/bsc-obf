/*
 * Decompiled with CFR 0.152.
 */
package com.google.common.graph;

import com.google.common.annotations.Beta;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import java.util.Set;
import javax.annotation.Nullable;

@Beta
public interface Network<N, E> {
    public Set<N> nodes();

    public Set<E> edges();

    public Graph<N> asGraph();

    public boolean isDirected();

    public boolean allowsParallelEdges();

    public boolean allowsSelfLoops();

    public ElementOrder<N> nodeOrder();

    public ElementOrder<E> edgeOrder();

    public Set<N> adjacentNodes(N var1);

    public Set<N> predecessors(N var1);

    public Set<N> successors(N var1);

    public Set<E> incidentEdges(N var1);

    public Set<E> inEdges(N var1);

    public Set<E> outEdges(N var1);

    public int degree(N var1);

    public int inDegree(N var1);

    public int outDegree(N var1);

    public EndpointPair<N> incidentNodes(E var1);

    public Set<E> adjacentEdges(E var1);

    public Set<E> edgesConnecting(N var1, N var2);

    public boolean equals(@Nullable Object var1);

    public int hashCode();
}

