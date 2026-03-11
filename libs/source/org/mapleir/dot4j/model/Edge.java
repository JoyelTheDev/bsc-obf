/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.model;

import java.util.Iterator;
import java.util.Map;
import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.MapAttrs;
import org.mapleir.dot4j.attr.SimpleAttributed;
import org.mapleir.dot4j.model.Context;
import org.mapleir.dot4j.model.Node;
import org.mapleir.dot4j.model.Source;
import org.mapleir.dot4j.model.Target;

public class Edge
implements Attributed<Edge>,
Target {
    private final Source<?> source;
    private final Target target;
    private final Attributed<Edge> attributes;

    public static Edge to(Node node) {
        return Edge.to(node.withRecord(null));
    }

    public static Edge to(Target to) {
        return Edge.makeEdge(null, to);
    }

    public static Edge between(Source<?> source, Target target) {
        return Edge.makeEdge(source, target);
    }

    private static Edge makeEdge(Source<?> source, Target target) {
        return Context.createEdge(source, target);
    }

    Edge(Source<?> source, Target target, Attrs attributes) {
        this.source = source;
        this.target = target;
        this.attributes = new SimpleAttributed<Edge>(this, attributes);
    }

    public Source<?> getSource() {
        return this.source;
    }

    public Target getTarget() {
        return this.target;
    }

    public Attributed<Edge> getAttrs() {
        return this.attributes;
    }

    @Override
    public Attrs applyTo(MapAttrs mapAttrs) {
        return this.attributes.applyTo(mapAttrs);
    }

    @Override
    public Edge edgeToHere() {
        return this;
    }

    @Override
    public Edge with(Attrs attrs) {
        this.attributes.with(attrs);
        return this;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Edge e = (Edge)o;
        return this.attributes.equals(e.attributes);
    }

    public int hashCode() {
        return this.attributes.hashCode();
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        throw new UnsupportedOperationException();
    }
}

