/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.objectweb.asm.tree.MethodNode
 */
package org.mapleir.asm;

import org.mapleir.asm.ClassNode;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.util.IHasJavaDesc;
import org.mapleir.stdlib.util.JavaDesc;

public class MethodNode
implements FastGraphVertex,
IHasJavaDesc {
    private static int ID_COUNTER = 1;
    private final int numericId = ID_COUNTER++;
    public final ClassNode owner;
    public final org.objectweb.asm.tree.MethodNode node;

    public MethodNode(org.objectweb.asm.tree.MethodNode node, ClassNode owner) {
        this.node = node;
        this.owner = owner;
    }

    public String toString() {
        return (this.owner != null ? this.getOwner() : "null") + "." + this.getName() + this.getDesc();
    }

    @Override
    public String getDisplayName() {
        return this.node.name;
    }

    @Override
    public int getNumericId() {
        return this.numericId;
    }

    @Override
    public String getOwner() {
        return this.owner.getName();
    }

    @Override
    public String getName() {
        return this.node.name;
    }

    @Override
    public String getDesc() {
        return this.node.desc;
    }

    @Override
    public JavaDesc.DescType getDescType() {
        return JavaDesc.DescType.METHOD;
    }

    @Override
    public JavaDesc getJavaDesc() {
        return new JavaDesc(this.owner.getName(), this.getName(), this.getDesc(), JavaDesc.DescType.METHOD);
    }

    public boolean isStatic() {
        return (this.node.access & 8) != 0;
    }

    public boolean isAbstract() {
        return (this.node.access & 0x400) != 0;
    }

    public boolean isNative() {
        return (this.node.access & 0x100) != 0;
    }
}

