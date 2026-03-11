package club.zaputivatel.swg.transformer.impl;

import club.zaputivatel.swg.transformer.Transformer;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.tree.ClassNode;

import java.util.Collections;
import java.util.Random;

public class ShuffleTransformer implements Transformer {

    private final Random random = new Random();

    @Override
    public void transform(ControlFlowGraph cfg, MethodNode mapleMethod) {

        ClassNode classNode = mapleMethod.owner.node;

        Collections.shuffle(classNode.fields, random);
        Collections.shuffle(classNode.methods, random);
    }
}