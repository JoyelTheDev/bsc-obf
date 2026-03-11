/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.objectweb.asm.tree.ClassNode
 *  org.objectweb.asm.tree.FieldNode
 *  org.objectweb.asm.tree.MethodNode
 */
package org.mapleir.asm;

import java.util.ArrayList;
import java.util.List;
import org.mapleir.asm.FieldNode;
import org.mapleir.asm.MethodNode;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;

public class ClassNode
implements FastGraphVertex {
    private static int ID_COUNTER = 1;
    private final int numericId = ID_COUNTER++;
    public final org.objectweb.asm.tree.ClassNode node;
    private final List<MethodNode> methods;
    private final List<FieldNode> fields;

    public ClassNode() {
        this.node = new org.objectweb.asm.tree.ClassNode();
        this.methods = new ArrayList<MethodNode>();
        this.fields = new ArrayList<FieldNode>();
    }

    ClassNode(org.objectweb.asm.tree.ClassNode node) {
        this.node = node;
        this.methods = new ArrayList<MethodNode>(node.methods.size());
        for (org.objectweb.asm.tree.MethodNode mn : node.methods) {
            this.methods.add(new MethodNode(mn, this));
        }
        this.fields = new ArrayList<FieldNode>(node.fields.size());
        for (org.objectweb.asm.tree.FieldNode fn : node.fields) {
            this.fields.add(new FieldNode(fn, this));
        }
    }

    public String getName() {
        return this.node.name;
    }

    public List<MethodNode> getMethods() {
        return this.methods;
    }

    public void addMethod(MethodNode mn) {
        this.methods.add(mn);
        this.node.methods.add(mn.node);
    }

    public List<FieldNode> getFields() {
        return this.fields;
    }

    @Override
    public String getDisplayName() {
        return this.node.name.replace("/", "_");
    }

    @Override
    public int getNumericId() {
        return this.numericId;
    }

    public boolean isEnum() {
        return (this.node.access & 0x4000) != 0;
    }

    public boolean isStatic() {
        return (this.node.access & 8) != 0;
    }

    public boolean isSynthetic() {
        return (this.node.access & 0x1000) != 0;
    }
}

