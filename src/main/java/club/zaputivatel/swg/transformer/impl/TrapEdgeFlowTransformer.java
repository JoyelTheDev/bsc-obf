package club.zaputivatel.swg.transformer.impl;

import club.zaputivatel.swg.transformer.Transformer;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class TrapEdgeFlowTransformer implements Transformer {

    @Override
    public void transform(ControlFlowGraph cfg, MethodNode mapleMethod) {
        org.objectweb.asm.tree.MethodNode method = mapleMethod.node;
        if (method.instructions == null || method.instructions.size() == 0) return;
        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) return;

        Map<AbstractInsnNode, Frame<BasicValue>> frames = analyzeFrames(method);
        if (frames == null || frames.isEmpty()) return;

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() != Opcodes.GOTO) continue;

            Frame<BasicValue> frame = frames.get(insn);
            if (frame == null) continue;
            if (frame.getStackSize() > 0) continue;

            JumpInsnNode gotoInsn = (JumpInsnNode) insn;
            LabelNode originalTarget = gotoInsn.label;

            LabelNode trapStart = new LabelNode();
            LabelNode trapEnd = new LabelNode();
            LabelNode handler = new LabelNode();

            InsnList list = new InsnList();
            list.add(new JumpInsnNode(Opcodes.GOTO, trapStart));
            list.add(trapStart);
            list.add(new InsnNode(Opcodes.ACONST_NULL));
            list.add(new InsnNode(Opcodes.ATHROW));
            list.add(trapEnd);
            list.add(new InsnNode(Opcodes.NOP));
            list.add(handler);
            list.add(new InsnNode(Opcodes.POP));
            list.add(new LdcInsnNode(ThreadLocalRandom.current().nextInt()));
            list.add(new InsnNode(Opcodes.POP));
            list.add(new JumpInsnNode(Opcodes.GOTO, originalTarget));

            method.tryCatchBlocks.add(new TryCatchBlockNode(
                    trapStart,
                    trapEnd,
                    handler,
                    null
            ));

            method.instructions.insertBefore(insn, list);
            method.instructions.remove(insn);
        }

        method.localVariables = null;
    }

    private Map<AbstractInsnNode, Frame<BasicValue>> analyzeFrames(org.objectweb.asm.tree.MethodNode method) {
        try {
            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());
            Frame<BasicValue>[] computed = analyzer.analyze("dummy/Owner", method);

            AbstractInsnNode[] insns = method.instructions.toArray();
            Map<AbstractInsnNode, Frame<BasicValue>> map = new HashMap<>(insns.length);

            for (int i = 0; i < insns.length; i++) {
                Frame<BasicValue> frame = computed[i];
                if (frame != null) {
                    map.put(insns[i], frame);
                }
            }

            return map;
        } catch (Exception ignored) {
            return null;
        }
    }
}