/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.objectweb.asm.tree.FieldNode
 */
package org.mapleir.asm;

import org.mapleir.asm.ClassNode;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.util.IHasJavaDesc;
import org.mapleir.stdlib.util.JavaDesc;

public class FieldNode
implements FastGraphVertex,
IHasJavaDesc {
    private static int ID_COUNTER = 1;
    private final int numericId = ID_COUNTER++;
    public final ClassNode owner;
    public final org.objectweb.asm.tree.FieldNode node;

    public FieldNode(org.objectweb.asm.tree.FieldNode node, ClassNode owner) {
        this.node = node;
        this.owner = owner;
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
        return JavaDesc.DescType.FIELD;
    }

    @Override
    public JavaDesc getJavaDesc() {
        return new JavaDesc(this.owner.getName(), this.getName(), this.getDesc(), JavaDesc.DescType.FIELD);
    }
}

