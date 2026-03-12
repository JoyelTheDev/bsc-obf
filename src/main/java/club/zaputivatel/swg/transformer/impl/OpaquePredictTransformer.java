package club.zaputivatel.swg.transformer.impl;

import club.zaputivatel.swg.transformer.Transformer;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.concurrent.ThreadLocalRandom;

public class OpaquePredictTransformer implements Transformer {

    @Override
    public void transform(ControlFlowGraph cfg, MethodNode mapleMethod) {
        org.objectweb.asm.tree.MethodNode method = mapleMethod.node;
        if (method.instructions == null) return;
        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) return;

        final Frame<BasicValue>[] frames;
        try {
            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicVerifier());
            frames = analyzer.analyze(mapleMethod.owner != null ? mapleMethod.owner.getName() : "java/lang/Object", method);
        } catch (Throwable t) {
            return;
        }

        int predicateLocal = method.maxLocals++;
        int scratchLocal = method.maxLocals++;
        int auxLocal = method.maxLocals++;
        int longLocal = method.maxLocals;
        method.maxLocals += 2;

        int seed = ThreadLocalRandom.current().nextInt();
        int seed2 = ThreadLocalRandom.current().nextInt();
        long lseed = ThreadLocalRandom.current().nextLong();

        InsnList init = new InsnList();
        init.add(new LdcInsnNode(seed));
        init.add(new VarInsnNode(Opcodes.ISTORE, predicateLocal));
        init.add(new LdcInsnNode(seed2));
        init.add(new VarInsnNode(Opcodes.ISTORE, scratchLocal));
        init.add(new LdcInsnNode(lseed));
        init.add(new VarInsnNode(Opcodes.LSTORE, longLocal));
        init.add(new InsnNode(Opcodes.ICONST_0));
        init.add(new VarInsnNode(Opcodes.ISTORE, auxLocal));
        method.instructions.insert(init);

        AbstractInsnNode[] insns = method.instructions.toArray();
        for (int i = 0; i < insns.length; i++) {
            AbstractInsnNode insn = insns[i];
            if (!(insn instanceof JumpInsnNode jmp)) continue;
            if (insn.getOpcode() != Opcodes.GOTO) continue;

            Frame<BasicValue> f = i < frames.length ? frames[i] : null;
            if (f == null || f.getStackSize() != 0) continue;

            if (ThreadLocalRandom.current().nextInt(100) >= 65) continue;

            LabelNode junk = new LabelNode();
            LabelNode real = jmp.label;

            boolean alwaysTrue = ThreadLocalRandom.current().nextBoolean();
            int k1 = ThreadLocalRandom.current().nextInt();
            int k2 = ThreadLocalRandom.current().nextInt(1, 1_000_000);
            long lk = ThreadLocalRandom.current().nextLong();

            InsnList repl = new InsnList();

            repl.add(new VarInsnNode(Opcodes.ILOAD, predicateLocal));
            repl.add(new LdcInsnNode(k1));
            repl.add(new InsnNode(Opcodes.IXOR));
            repl.add(new VarInsnNode(Opcodes.ISTORE, scratchLocal));

            repl.add(new VarInsnNode(Opcodes.ILOAD, scratchLocal));
            repl.add(new LdcInsnNode(k2));
            repl.add(new InsnNode(Opcodes.IADD));
            repl.add(new VarInsnNode(Opcodes.ISTORE, auxLocal));

            repl.add(new VarInsnNode(Opcodes.LLOAD, longLocal));
            repl.add(new LdcInsnNode(lk));
            repl.add(new InsnNode(Opcodes.LXOR));
            repl.add(new VarInsnNode(Opcodes.LSTORE, longLocal));

            repl.add(new IincInsnNode(predicateLocal, ThreadLocalRandom.current().nextInt(-3, 4)));

            repl.add(new VarInsnNode(Opcodes.ILOAD, auxLocal));
            repl.add(new VarInsnNode(Opcodes.ILOAD, auxLocal));
            repl.add(new InsnNode(Opcodes.ISUB));

            if (alwaysTrue) {
                repl.add(new JumpInsnNode(Opcodes.IFEQ, real));
            } else {
                repl.add(new JumpInsnNode(Opcodes.IFNE, junk));
            }

            repl.add(new JumpInsnNode(Opcodes.GOTO, junk));
            repl.add(junk);

            repl.add(new VarInsnNode(Opcodes.ILOAD, scratchLocal));
            repl.add(new VarInsnNode(Opcodes.ILOAD, predicateLocal));
            repl.add(new InsnNode(Opcodes.IXOR));
            repl.add(new VarInsnNode(Opcodes.ISTORE, scratchLocal));

            repl.add(new InsnNode(Opcodes.NOP));
            repl.add(new JumpInsnNode(Opcodes.GOTO, real));

            method.instructions.insertBefore(insn, repl);
            method.instructions.remove(insn);
        }

        method.localVariables = null;
        method.instructions.resetLabels();
    }
}
