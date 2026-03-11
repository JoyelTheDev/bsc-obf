/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.model;

import org.mapleir.dot4j.model.Compass;
import org.mapleir.dot4j.model.Edge;
import org.mapleir.dot4j.model.Node;
import org.mapleir.dot4j.model.Source;
import org.mapleir.dot4j.model.Target;

public class PortNode
implements Target,
Source<Node> {
    protected Node node;
    protected String record;
    protected Compass position;

    public PortNode() {
    }

    public PortNode(Node node, String record, Compass position) {
        this.node = node;
        this.record = record;
        this.position = position;
    }

    public PortNode copy() {
        return new PortNode(this.node, this.record, this.position);
    }

    public PortNode setNode(Node node) {
        this.node = node;
        return this;
    }

    public PortNode setRecord(String record) {
        this.record = record;
        return this;
    }

    public PortNode setCompass(Compass position) {
        this.position = position;
        return this;
    }

    public Node getNode() {
        return this.node;
    }

    public String getRecord() {
        return this.record;
    }

    public Compass getPosition() {
        return this.position;
    }

    @Override
    public Node addEdge(Target target) {
        return this.node.addEdge(target);
    }

    @Override
    public Edge edgeToHere() {
        return Edge.to(this);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        PortNode n = (PortNode)o;
        if (this.node != null ? !this.node.equals(n.node) : n.node != null) {
            return false;
        }
        if (this.record != null ? !this.record.equals(n.record) : n.record != null) {
            return false;
        }
        return this.position == n.position;
    }

    public int hashCode() {
        int result = this.node != null ? this.node.hashCode() : 0;
        result = 31 * result + (this.record != null ? this.record.hashCode() : 0);
        result = 31 * result + (this.position != null ? this.position.hashCode() : 0);
        return result;
    }

    public String toString() {
        return (this.record == null ? "" : this.record) + ":" + (this.position == null ? "" : this.position) + ":" + this.node.toString();
    }
}

