/*
 * Decompiled with CFR 0.152.
 */
package com.google.common.graph;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.graph.AbstractGraph;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.Network;
import com.google.common.math.IntMath;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

@Beta
public abstract class AbstractNetwork<N, E>
implements Network<N, E> {
    @Override
    public Graph<N> asGraph() {
        return new AbstractGraph<N>(){

            @Override
            public Set<N> nodes() {
                return AbstractNetwork.this.nodes();
            }

            @Override
            public Set<EndpointPair<N>> edges() {
                if (AbstractNetwork.this.allowsParallelEdges()) {
                    return super.edges();
                }
                return new AbstractSet<EndpointPair<N>>(){

                    @Override
                    public Iterator<EndpointPair<N>> iterator() {
                        return Iterators.transform(AbstractNetwork.this.edges().iterator(), new Function<E, EndpointPair<N>>(){

                            @Override
                            public EndpointPair<N> apply(E edge) {
                                return AbstractNetwork.this.incidentNodes(edge);
                            }
                        });
                    }

                    @Override
                    public int size() {
                        return AbstractNetwork.this.edges().size();
                    }

                    @Override
                    public boolean contains(@Nullable Object obj) {
                        if (!(obj instanceof EndpointPair)) {
                            return false;
                        }
                        EndpointPair endpointPair = (EndpointPair)obj;
                        return this.isDirected() == endpointPair.isOrdered() && this.nodes().contains(endpointPair.nodeU()) && this.successors(endpointPair.nodeU()).contains(endpointPair.nodeV());
                    }
                };
            }

            @Override
            public ElementOrder<N> nodeOrder() {
                return AbstractNetwork.this.nodeOrder();
            }

            @Override
            public boolean isDirected() {
                return AbstractNetwork.this.isDirected();
            }

            @Override
            public boolean allowsSelfLoops() {
                return AbstractNetwork.this.allowsSelfLoops();
            }

            @Override
            public Set<N> adjacentNodes(N node) {
                return AbstractNetwork.this.adjacentNodes(node);
            }

            @Override
            public Set<N> predecessors(N node) {
                return AbstractNetwork.this.predecessors(node);
            }

            @Override
            public Set<N> successors(N node) {
                return AbstractNetwork.this.successors(node);
            }
        };
    }

    @Override
    public int degree(N node) {
        if (this.isDirected()) {
            return IntMath.saturatedAdd(this.inEdges(node).size(), this.outEdges(node).size());
        }
        return IntMath.saturatedAdd(this.incidentEdges(node).size(), this.edgesConnecting(node, node).size());
    }

    @Override
    public int inDegree(N node) {
        return this.isDirected() ? this.inEdges(node).size() : this.degree(node);
    }

    @Override
    public int outDegree(N node) {
        return this.isDirected() ? this.outEdges(node).size() : this.degree(node);
    }

    @Override
    public Set<E> adjacentEdges(E edge) {
        EndpointPair endpointPair = this.incidentNodes(edge);
        Sets.SetView endpointPairIncidentEdges = Sets.union(this.incidentEdges(endpointPair.nodeU()), this.incidentEdges(endpointPair.nodeV()));
        return Sets.difference(endpointPairIncidentEdges, ImmutableSet.of(edge));
    }

    @Override
    public final boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Network)) {
            return false;
        }
        Network other = (Network)obj;
        return this.isDirected() == other.isDirected() && this.nodes().equals(other.nodes()) && AbstractNetwork.edgeIncidentNodesMap(this).equals(AbstractNetwork.edgeIncidentNodesMap(other));
    }

    @Override
    public final int hashCode() {
        return AbstractNetwork.edgeIncidentNodesMap(this).hashCode();
    }

    public String toString() {
        String propertiesString = String.format("isDirected: %s, allowsParallelEdges: %s, allowsSelfLoops: %s", this.isDirected(), this.allowsParallelEdges(), this.allowsSelfLoops());
        return String.format("%s, nodes: %s, edges: %s", propertiesString, this.nodes(), AbstractNetwork.edgeIncidentNodesMap(this));
    }

    private static <N, E> Map<E, EndpointPair<N>> edgeIncidentNodesMap(final Network<N, E> network) {
        Function edgeToIncidentNodesFn = new Function<E, EndpointPair<N>>(){

            @Override
            public EndpointPair<N> apply(E edge) {
                return network.incidentNodes(edge);
            }
        };
        return Maps.asMap(network.edges(), edgeToIncidentNodesFn);
    }
}

