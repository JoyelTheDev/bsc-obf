/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.MapAttrs;
import org.mapleir.dot4j.attr.SimpleAttributed;
import org.mapleir.dot4j.attr.builtin.ComplexLabel;
import org.mapleir.dot4j.model.Compass;
import org.mapleir.dot4j.model.Connected;
import org.mapleir.dot4j.model.Edge;
import org.mapleir.dot4j.model.Factory;
import org.mapleir.dot4j.model.PortNode;
import org.mapleir.dot4j.model.Source;
import org.mapleir.dot4j.model.Target;

public class Node
implements Attributed<Node>,
Source<Node>,
Target,
Connected {
    protected ComplexLabel name;
    protected List<Edge> edges;
    protected Attributed<Node> attributes;

    Node() {
        this(null, new ArrayList<Edge>(), Attrs.attrs(new Attrs[0]));
    }

    public Node(ComplexLabel name, List<Edge> edges, Attrs attributes) {
        this.edges = edges;
        this.attributes = new SimpleAttributed<Node>(this, attributes);
        this.setName(name);
    }

    public ComplexLabel getName() {
        return this.name;
    }

    public Node copy() {
        return new Node(this.name, new ArrayList<Edge>(this.edges), this.attributes.applyTo(Attrs.attrs(new Attrs[0])));
    }

    public Node setName(ComplexLabel name) {
        this.name = name;
        if (name != null) {
            if (name.isExternal()) {
                this.name = ComplexLabel.of("");
                this.attributes.with((Attrs)name);
            } else if (name.isHtml()) {
                this.attributes.with((Attrs)name);
            }
        }
        return this;
    }

    public Node setName(String name) {
        return this.setName(ComplexLabel.of(name));
    }

    public Node merge(Node n) {
        this.edges.addAll(n.edges);
        this.attributes.with((Attrs)n.attributes);
        return this;
    }

    public PortNode withRecord(String record) {
        return new PortNode().setNode(this).setRecord(record);
    }

    public PortNode withPosition(Compass position) {
        return new PortNode().setNode(this).setCompass(position);
    }

    @Override
    public Attrs applyTo(MapAttrs mapAttrs) {
        return this.attributes.applyTo(mapAttrs);
    }

    @Override
    public Node with(Attrs attrs) {
        this.attributes.with(attrs);
        return this;
    }

    @Override
    public Object get(String key) {
        return this.attributes.get(key);
    }

    @Override
    public Collection<Edge> getEdges() {
        return this.edges;
    }

    @Override
    public Edge edgeToHere() {
        return Edge.to(this);
    }

    @Override
    public Node addEdge(Target target) {
        Edge edge = target.edgeToHere();
        this.edges.add(Edge.between(this.getSource(edge), edge.getTarget()).with((Attrs)edge.getAttrs()));
        return this;
    }

    public Node addEdge(String node) {
        return this.addEdge(Factory.node(this.name));
    }

    public Node addEdges(Target ... targets) {
        for (Target target : targets) {
            this.addEdge(target);
        }
        return this;
    }

    public Node addEdges(String ... names) {
        for (String name : names) {
            this.addEdge(name);
        }
        return this;
    }

    private Source<?> getSource(Edge edge) {
        if (edge.getSource() instanceof PortNode) {
            PortNode n = (PortNode)edge.getSource();
            return new PortNode().setNode(this).setRecord(n.getRecord()).setCompass(n.getPosition());
        }
        return new PortNode().setNode(this);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Node node = (Node)o;
        if (this.name != null ? !this.name.equals(node.name) : node.name != null) {
            return false;
        }
        if (this.edges != null ? !this.edges.equals(node.edges) : node.edges != null) {
            return false;
        }
        return !(this.attributes == null ? node.attributes != null : !this.attributes.equals(node.attributes));
    }

    public int hashCode() {
        int result = this.name != null ? this.name.hashCode() : 0;
        result = 31 * result + (this.edges != null ? this.edges.hashCode() : 0);
        result = 31 * result + (this.attributes != null ? this.attributes.hashCode() : 0);
        return result;
    }

    public String toString() {
        return this.name + this.attributes.toString() + "->" + this.edges.stream().map(l -> l.getTarget().toString()).collect(Collectors.joining(","));
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return this.attributes.iterator();
    }
}

