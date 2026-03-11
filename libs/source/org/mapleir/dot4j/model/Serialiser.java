/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.builtin.Label;
import org.mapleir.dot4j.model.Connected;
import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.dot4j.model.Edge;
import org.mapleir.dot4j.model.Node;
import org.mapleir.dot4j.model.PortNode;
import org.mapleir.dot4j.model.Target;

public class Serialiser {
    private final DotGraph graph;
    private final StringBuilder str;

    public Serialiser(DotGraph graph) {
        this.graph = graph;
        this.str = new StringBuilder();
    }

    public String serialise() {
        this.graph(this.graph, true);
        return this.str.toString();
    }

    private void graph(DotGraph graph, boolean toplevel) {
        this.graphInit(graph, toplevel);
        this.graphAttrs(graph);
        ArrayList<Node> nodes = new ArrayList<Node>();
        ArrayList<DotGraph> graphs = new ArrayList<DotGraph>();
        Collection<Connected> linkables = this.linkedNodes(graph.nodes);
        linkables.addAll(this.linkedNodes(graph.subgraphs));
        for (Connected linkable : linkables) {
            if (linkable instanceof Node) {
                Node node = (Node)linkable;
                int i = this.indexOfName(nodes, node.name);
                if (i < 0) {
                    nodes.add(node);
                    continue;
                }
                nodes.set(i, node.copy().merge((Node)nodes.get(i)));
                continue;
            }
            graphs.add((DotGraph)linkable);
        }
        this.nodes(graph, nodes);
        this.graphs(graphs, nodes);
        this.edges(nodes);
        this.edges(graphs);
        this.str.append('}');
    }

    private void graphAttrs(DotGraph graph) {
        this.attributes("graph", graph.graphAttrs);
        this.attributes("node", graph.nodeAttrs);
        this.attributes("edge", graph.edgeAttrs);
    }

    private void graphInit(DotGraph graph, boolean toplevel) {
        if (toplevel) {
            this.str.append(graph.strict ? "strict " : "").append(graph.directed ? "digraph " : "graph ");
            if (!graph.name.isEmpty()) {
                this.str.append(Label.of(graph.name).serialised()).append(' ');
            }
        } else if (!graph.name.isEmpty() || graph.cluster) {
            this.str.append("subgraph ").append(Label.of((graph.cluster ? "cluster_" : "") + graph.name).serialised()).append(' ');
        }
        this.str.append("{\n");
    }

    private int indexOfName(List<Node> nodes, Label name) {
        for (int i = 0; i < nodes.size(); ++i) {
            if (!nodes.get((int)i).name.equals(name)) continue;
            return i;
        }
        return -1;
    }

    private void attributes(String name, Attributed<?> attributed) {
        if (!attributed.isEmpty()) {
            this.str.append(name);
            this.attrs(attributed);
            this.str.append('\n');
        }
    }

    private Collection<Connected> linkedNodes(Collection<? extends Connected> nodes) {
        LinkedHashSet<Connected> visited = new LinkedHashSet<Connected>();
        for (Connected connected : nodes) {
            this.linkedNodes(connected, visited);
        }
        return visited;
    }

    private void linkedNodes(Connected linkable, Set<Connected> visited) {
        if (!visited.contains(linkable)) {
            visited.add(linkable);
            for (Edge link : linkable.getEdges()) {
                Target target = link.getTarget();
                if (target instanceof Node) {
                    this.linkedNodes((Node)target, visited);
                    continue;
                }
                if (target instanceof PortNode) {
                    this.linkedNodes(((PortNode)target).node, visited);
                    continue;
                }
                if (target instanceof DotGraph) {
                    this.linkedNodes((DotGraph)target, visited);
                    continue;
                }
                throw new IllegalStateException("unexpected link to " + link.getTarget() + " of " + link.getTarget().getClass());
            }
        }
    }

    private void nodes(DotGraph graph, List<Node> nodes) {
        for (Node node : nodes) {
            if (node.attributes.isEmpty() && (!graph.nodes.contains(node) || !node.getEdges().isEmpty())) continue;
            this.node(node);
            this.str.append('\n');
        }
    }

    private void graphs(List<DotGraph> graphs, List<Node> nodes) {
        for (DotGraph graph : graphs) {
            if (!graph.getEdges().isEmpty() || this.isLinked(graph, nodes) || this.isLinked(graph, graphs)) continue;
            this.graph(graph, false);
            this.str.append('\n');
        }
    }

    private boolean isLinked(DotGraph graph, List<? extends Connected> linkables) {
        for (Connected connected : linkables) {
            for (Edge link : connected.getEdges()) {
                if (!link.getTarget().equals(graph)) continue;
                return true;
            }
        }
        return false;
    }

    private void edges(List<? extends Connected> linkables) {
        for (Connected connected : linkables) {
            for (Edge link : connected.getEdges()) {
                this.linkTarget(link.getSource());
                this.str.append(this.graph.directed ? " -> " : " -- ");
                this.linkTarget(link.getTarget());
                this.attrs(link.getAttrs());
                this.str.append('\n');
            }
        }
    }

    private void linkTarget(Object linkable) {
        if (linkable instanceof Node) {
            this.node((Node)linkable);
        } else if (linkable instanceof PortNode) {
            this.port((PortNode)linkable);
        } else if (linkable instanceof DotGraph) {
            this.graph((DotGraph)linkable, false);
        } else {
            throw new IllegalStateException("unexpected link target " + linkable);
        }
    }

    private void node(Node node) {
        this.str.append(node.name.serialised());
        this.attrs(node.attributes);
    }

    private void port(PortNode portNode) {
        this.str.append(portNode.node.name.serialised());
        if (portNode.record != null) {
            this.str.append(':').append(Label.of(portNode.record).serialised());
        }
        if (portNode.getPosition() != null) {
            this.str.append(':').append(portNode.getPosition().value);
        }
    }

    private void attrs(Attributed<?> attrs) {
        if (!attrs.isEmpty()) {
            this.str.append(" [");
            boolean first = true;
            for (Map.Entry entry : attrs) {
                if (first) {
                    first = false;
                } else {
                    this.str.append(',');
                }
                this.attr((String)entry.getKey(), entry.getValue());
            }
            this.str.append(']');
        }
    }

    private void attr(String key, Object value) {
        this.str.append(Label.of(key).serialised()).append('=').append(Label.of(value).serialised());
    }
}

