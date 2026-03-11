/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.model;

import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.builtin.ComplexLabel;
import org.mapleir.dot4j.model.Compass;
import org.mapleir.dot4j.model.Context;
import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.dot4j.model.Edge;
import org.mapleir.dot4j.model.Node;
import org.mapleir.dot4j.model.PortNode;
import org.mapleir.dot4j.model.Target;

public final class Factory {
    private Factory() {
    }

    public static DotGraph graph(String name) {
        return Factory.graph().setName(name);
    }

    public static DotGraph graph() {
        return Context.createGraph();
    }

    public static Node node(String name) {
        return Factory.node(ComplexLabel.of(name));
    }

    public static Node node(ComplexLabel label) {
        return Context.createNode(label);
    }

    public static Edge to(Node node) {
        return Edge.to(node);
    }

    public static Edge to(Target target) {
        return Edge.to(target);
    }

    public static PortNode portNode(String record) {
        return new PortNode().setRecord(record);
    }

    public static PortNode portNode(Compass position) {
        return new PortNode().setCompass(position);
    }

    public static PortNode portNode(String record, Compass position) {
        return new PortNode().setRecord(record).setCompass(position);
    }

    public static Attributed<?> nodeAttrs() {
        return Context.get().nodeAttrs();
    }

    public static Attributed<?> edgeAttrs() {
        return Context.get().edgeAttrs();
    }

    public static Attributed<?> graphAttrs() {
        return Context.get().graphAttrs();
    }
}

