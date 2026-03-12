package club.zaputivatel.swg.transformer.impl;

import club.zaputivatel.swg.transformer.Transformer;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockBreakerTransformer implements Transformer {

    @Override
    public void transform(ControlFlowGraph cfg, MethodNode mapleMethod) {
        org.objectweb.asm.tree.MethodNode method = mapleMethod.node;
        if (method.instructions == null || method.instructions.size() == 0) return;
        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) return;

        if (cfg == null) return;

        for (org.mapleir.ir.cfg.BasicBlock block : cfg.vertices()) {
            List<AbstractInsnNode> insns = _collectAsmInstructions(block);
            if (insns.size() < 2) continue;

            List<List<AbstractInsnNode>> sameFrames = _groupSameFrames(cfg, insns);
            if (sameFrames.isEmpty()) continue;

            _filterTooClose(sameFrames, method);
            sameFrames.removeIf(list -> list.size() < 2 || list.size() >= 20);

            for (List<AbstractInsnNode> group : sameFrames) {
                for (AbstractInsnNode insn : group) {
                    method.instructions.insertBefore(insn, new LabelNode());
                }
            }
        }

        method.localVariables = null;
        method.instructions.resetLabels();
    }

    private List<AbstractInsnNode> _collectAsmInstructions(org.mapleir.ir.cfg.BasicBlock block) {
        List<AbstractInsnNode> result = new ArrayList<>();
        for (Object unit : block) {
            if (unit instanceof AbstractInsnNode insn) {
                result.add(insn);
            }
        }
        return result;
    }

    private List<List<AbstractInsnNode>> _groupSameFrames(ControlFlowGraph cfg, List<AbstractInsnNode> insns) {
        try {
            Object grouped = cfg.getClass()
                .getMethod("groupSameFrames", List.class)
                .invoke(cfg, insns);

            if (grouped instanceof List<?> raw) {
                List<List<AbstractInsnNode>> result = new ArrayList<>();
                for (Object o : raw) {
                    if (o instanceof List<?> rawList) {
                        List<AbstractInsnNode> casted = new ArrayList<>();
                        for (Object e : rawList) {
                            if (e instanceof AbstractInsnNode ain) {
                                casted.add(ain);
                            }
                        }
                        if (!casted.isEmpty()) {
                            result.add(casted);
                        }
                    }
                }
                return result;
            }
        } catch (Throwable ignored) {
        }

        return new ArrayList<>();
    }

    private void _filterTooClose(List<List<AbstractInsnNode>> groups, org.objectweb.asm.tree.MethodNode method) {
        for (List<AbstractInsnNode> list : groups) {
            Iterator<AbstractInsnNode> it = list.iterator();
            while (it.hasNext()) {
                AbstractInsnNode insn = it.next();
                int idx = method.instructions.indexOf(insn);

                boolean tooClose = false;
                for (AbstractInsnNode other : list) {
                    if (insn == other) continue;

                    int otherIdx = method.instructions.indexOf(other);
                    if (Math.abs(idx - otherIdx) < 2) {
                        tooClose = true;
                        break;
                    }
                }

                if (tooClose) {
                    it.remove();
                }
            }
        }
    }
}