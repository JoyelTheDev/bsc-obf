/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.builtin.Font;
import org.mapleir.dot4j.model.Context;
import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.dot4j.model.Edge;
import org.mapleir.dot4j.model.Factory;
import org.mapleir.dot4j.model.Node;
import org.mapleir.stdlib.collections.graph.FastDirectedGraph;
import org.mapleir.stdlib.collections.graph.FastGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.collections.graph.algorithms.ExtendedDfs;
import org.mapleir.stdlib.collections.graph.algorithms.LT79Dom;

public class GraphUtils {
    private static final Attrs DEFAULT_FONT = Font.config("consolas bold", 8.0, 200.0);

    public static Attrs getDefaultFont() {
        return Attrs.attrs(DEFAULT_FONT);
    }

    public static <N extends FastGraphVertex> DotGraph makeDotSkeleton(FastGraph<N, ? extends FastGraphEdge<N>> fastGraph) {
        return GraphUtils.makeDotSkeleton(fastGraph, null, null);
    }

    public static <N extends FastGraphVertex, E extends FastGraphEdge<N>> DotGraph makeDotSkeleton(FastGraph<N, E> fastGraph, BiPredicate<N, Node> nodePredicate, BiPredicate<E, Edge> edgePredicate) {
        Attrs font = GraphUtils.getDefaultFont();
        DotGraph graph = Factory.graph().getGraphAttr().with(font).getNodeAttr().with(font).getEdgeAttr().with(font);
        return Context.use(graph, ctx -> {
            for (FastGraphVertex vertex : fastGraph.vertices()) {
                Node sourceNode = Factory.node(vertex.getDisplayName());
                boolean addNode = true;
                if (nodePredicate != null) {
                    addNode = nodePredicate.test(vertex, sourceNode);
                }
                if (addNode) {
                    graph.addSource(sourceNode);
                }
                for (FastGraphEdge e : fastGraph.getEdges(vertex)) {
                    Node target = Factory.node(e.dst().getDisplayName());
                    Edge edge = Factory.to(target);
                    boolean addEdge = true;
                    if (edgePredicate != null) {
                        addEdge = edgePredicate.test(e, edge);
                    }
                    if (!addEdge) continue;
                    sourceNode.addEdge(edge);
                }
            }
            return graph;
        });
    }

    public static void removeNodeEdges(DotGraph graph, BiPredicate<Node, Edge> keepEdgePredicate) {
        Context.use(graph, ctx -> {
            for (Node node : graph.getAllNodes()) {
                Iterator<Edge> edgeIt = node.getEdges().iterator();
                while (edgeIt.hasNext()) {
                    Edge edge = edgeIt.next();
                    if (keepEdgePredicate.test(node, edge)) continue;
                    edgeIt.remove();
                }
            }
            return graph;
        });
    }

    public static <N extends FastGraphVertex> String toNodeArray(Collection<N> col) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        Iterator<N> it = col.iterator();
        while (it.hasNext()) {
            FastGraphVertex b = (FastGraphVertex)it.next();
            if (b == null) {
                sb.append("null");
            } else {
                sb.append(b.getDisplayName());
            }
            if (!it.hasNext()) continue;
            sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    public static int getEdgeCount(FastGraph<FastGraphVertex, ?> g) {
        int c = 0;
        for (FastGraphVertex v : g.vertices()) {
            c += g.getEdges(v).size();
        }
        return c;
    }

    @Deprecated
    public static <N extends FastGraphVertex, E extends FastGraphEdge<N>> int compareEdgesById(E a, E b) {
        if (a.equals(b)) {
            return 0;
        }
        int result = Integer.compare(a.src().getNumericId(), b.src().getNumericId());
        if (result == 0) {
            result = Integer.compare(a.dst().getNumericId(), b.dst().getNumericId());
        }
        assert (result != 0);
        return result;
    }

    public static <N extends FastGraphVertex, E extends FastGraphEdge<N>> boolean isReducibleGraph(FastDirectedGraph<N, E> g, N entry) {
        HashSet<FastGraphEdge> backEdges = new HashSet<FastGraphEdge>();
        LT79Dom dom = new LT79Dom(g, entry);
        for (FastGraphVertex b : g.vertices()) {
            for (FastGraphEdge edge : g.getEdges(b)) {
                if (!dom.getDominates(edge.dst()).contains(b)) continue;
                backEdges.add(edge);
            }
        }
        Set<FastGraphEdge<N>> retreatingEdges = new ExtendedDfs<N>(g, 1).run(entry).getEdges(1);
        retreatingEdges.removeAll(backEdges);
        return retreatingEdges.isEmpty();
    }

    public static <N extends FastGraphVertex, E extends FastGraphEdge<N>, G extends FastGraph<N, E>> G inducedSubgraph(G g, Collection<N> vertices, Supplier<G> factory) {
        FastGraph subgraph = (FastGraph)factory.get();
        for (FastGraphVertex n : vertices) {
            subgraph.addVertex(n);
            for (FastGraphEdge e : g.getEdges((FastGraphVertex)n)) {
                if (!vertices.contains(e.dst())) continue;
                subgraph.addEdge(e);
            }
        }
        return (G)subgraph;
    }
}

