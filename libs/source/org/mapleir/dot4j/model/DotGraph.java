/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.SimpleAttributed;
import org.mapleir.dot4j.model.Connected;
import org.mapleir.dot4j.model.Context;
import org.mapleir.dot4j.model.Edge;
import org.mapleir.dot4j.model.Node;
import org.mapleir.dot4j.model.PortNode;
import org.mapleir.dot4j.model.Serialiser;
import org.mapleir.dot4j.model.Source;
import org.mapleir.dot4j.model.Target;

public class DotGraph
implements Source<DotGraph>,
Target,
Connected {
    protected boolean strict;
    protected boolean directed;
    protected boolean cluster;
    protected String name;
    protected final Set<Node> nodes;
    protected final Set<DotGraph> subgraphs;
    protected final List<Edge> edges;
    protected final Attributed<DotGraph> nodeAttrs;
    protected final Attributed<DotGraph> edgeAttrs;
    protected final Attributed<DotGraph> graphAttrs;

    DotGraph() {
        this(false, false, false, "", new LinkedHashSet<Node>(), new LinkedHashSet<DotGraph>(), new ArrayList<Edge>(), null, null, null);
        Context.current().ifPresent(ctx -> this.getGraphAttr().with((Attrs)ctx.graphAttrs()));
    }

    public DotGraph(boolean strict, boolean directed, boolean cluster, String name, Set<Node> nodes, Set<DotGraph> subgraphs, List<Edge> edges, Attrs nodeAttrs, Attrs edgeAttrs, Attrs graphAttrs) {
        this.strict = strict;
        this.directed = directed;
        this.cluster = cluster;
        this.name = name;
        this.nodes = nodes;
        this.subgraphs = subgraphs;
        this.edges = edges;
        this.nodeAttrs = new SimpleAttributed<DotGraph>(this, nodeAttrs);
        this.edgeAttrs = new SimpleAttributed<DotGraph>(this, edgeAttrs);
        this.graphAttrs = new SimpleAttributed<DotGraph>(this, graphAttrs);
    }

    public DotGraph copy() {
        return new DotGraph(this.strict, this.directed, this.cluster, this.name, new LinkedHashSet<Node>(this.nodes), new LinkedHashSet<DotGraph>(this.subgraphs), new ArrayList<Edge>(this.edges), this.nodeAttrs, this.edgeAttrs, this.graphAttrs);
    }

    public DotGraph addEdges(Target ... targets) {
        for (Target target : targets) {
            this.addEdge(target);
        }
        return this;
    }

    @Override
    public DotGraph addEdge(Target target) {
        Edge edge = target.edgeToHere();
        this.edges.add(Edge.between(this, edge.getTarget()).with((Attrs)edge.getAttrs()));
        return this;
    }

    @Override
    public Edge edgeToHere() {
        return Edge.to(this);
    }

    @Override
    public Collection<Edge> getEdges() {
        return this.edges;
    }

    public DotGraph setStrict(boolean strict) {
        this.strict = strict;
        return this;
    }

    public DotGraph setDirected(boolean directed) {
        this.directed = directed;
        return this;
    }

    public DotGraph setClustered(boolean cluster) {
        this.cluster = cluster;
        return this;
    }

    public DotGraph setName(String name) {
        this.name = name;
        return this;
    }

    public DotGraph addSource(Source<?> source) {
        if (source instanceof Node) {
            this.nodes.add((Node)source);
            return this;
        }
        if (source instanceof PortNode) {
            this.nodes.add(((PortNode)source).getNode());
            return this;
        }
        if (source instanceof DotGraph) {
            this.subgraphs.add((DotGraph)source);
            return this;
        }
        throw new IllegalArgumentException("Unknown source of type " + source.getClass());
    }

    public DotGraph addSources(Source<?> ... sources) {
        for (Source<?> source : sources) {
            this.addSource(source);
        }
        return this;
    }

    public Collection<Node> getRootNodes() {
        return this.nodes;
    }

    public Collection<Node> getAllNodes() {
        HashSet<Node> set = new HashSet<Node>();
        for (Node n : this.nodes) {
            this.addNodes(n, set);
        }
        return set;
    }

    private void addNodes(Node node, Set<Node> vis) {
        if (!vis.contains(node)) {
            vis.add(node);
            for (Edge e : node.getEdges()) {
                if (!(e.getTarget() instanceof PortNode)) continue;
                this.addNodes(((PortNode)e.getTarget()).getNode(), vis);
            }
        }
    }

    public boolean isStrict() {
        return this.strict;
    }

    public boolean isDirected() {
        return this.directed;
    }

    public boolean isClustered() {
        return this.cluster;
    }

    public String getName() {
        return this.name;
    }

    public Attributed<DotGraph> getNodeAttr() {
        return this.nodeAttrs;
    }

    public Attributed<DotGraph> getEdgeAttr() {
        return this.edgeAttrs;
    }

    public Attributed<DotGraph> getGraphAttr() {
        return this.graphAttrs;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        DotGraph graph = (DotGraph)o;
        if (this.strict != graph.strict) {
            return false;
        }
        if (this.directed != graph.directed) {
            return false;
        }
        if (this.cluster != graph.cluster) {
            return false;
        }
        if (!this.name.equals(graph.name)) {
            return false;
        }
        if (!this.nodes.equals(graph.nodes)) {
            return false;
        }
        if (!this.subgraphs.equals(graph.subgraphs)) {
            return false;
        }
        if (!this.edges.equals(graph.edges)) {
            return false;
        }
        if (!this.nodeAttrs.equals(graph.nodeAttrs)) {
            return false;
        }
        if (!this.edgeAttrs.equals(graph.edgeAttrs)) {
            return false;
        }
        return this.graphAttrs.equals(graph.graphAttrs);
    }

    public int hashCode() {
        int result = this.strict ? 1 : 0;
        result = 31 * result + (this.directed ? 1 : 0);
        result = 31 * result + (this.cluster ? 1 : 0);
        result = 31 * result + this.name.hashCode();
        result = 31 * result + this.nodes.hashCode();
        result = 31 * result + this.subgraphs.hashCode();
        result = 31 * result + this.edges.hashCode();
        result = 31 * result + this.nodeAttrs.hashCode();
        result = 31 * result + this.edgeAttrs.hashCode();
        result = 31 * result + this.graphAttrs.hashCode();
        return result;
    }

    public String toString() {
        return new Serialiser(this).serialise();
    }
}

