package club.zaputivatel.swg.transformer.impl;

import club.zaputivatel.swg.transformer.Transformer;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

public class BlockDuplicateTransformer implements Transformer {

    public BlockDuplicateTransformer() {}

    @Override
    public void transform(ControlFlowGraph cfg, MethodNode mapleMethod) {
        org.objectweb.asm.tree.MethodNode method = mapleMethod.node;
        if (method.instructions == null || method.instructions.size() < 10) return;
        if (method.name.equals("<init>") || (method.access & Opcodes.ACC_ABSTRACT) != 0) return;

        Map<LabelNode, LabelNode> labelMap = new HashMap<>();
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof LabelNode) labelMap.put((LabelNode) insn, new LabelNode());
        }

        if (method.tryCatchBlocks != null && !method.tryCatchBlocks.isEmpty()) {
            for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
                if (!(tcb.start instanceof LabelNode) || !(tcb.end instanceof LabelNode) || !(tcb.handler instanceof LabelNode)) {
                    return;
                }
                if (!labelMap.containsKey((LabelNode) tcb.start) ||
                        !labelMap.containsKey((LabelNode) tcb.end) ||
                        !labelMap.containsKey((LabelNode) tcb.handler)) {
                    return;
                }
            }
        }

        List<List<AbstractInsnNode>> blocks = splitIntoBlocks(method.instructions);
        InsnList newInsns = new InsnList();

        for (List<AbstractInsnNode> block : blocks) {

            if (shouldDuplicate(block)) {

                LabelNode cloneLabel = new LabelNode();
                LabelNode endLabel = new LabelNode();

                newInsns.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "java/util/concurrent/ThreadLocalRandom",
                        "current",
                        "()Ljava/util/concurrent/ThreadLocalRandom;",
                        false));

                newInsns.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/util/concurrent/ThreadLocalRandom",
                        "nextBoolean",
                        "()Z",
                        false));

                newInsns.add(new JumpInsnNode(Opcodes.IFNE, cloneLabel));

                for (AbstractInsnNode insn : block) {
                    newInsns.add(insn.clone(labelMap));
                }

                newInsns.add(new JumpInsnNode(Opcodes.GOTO, endLabel));

                newInsns.add(cloneLabel);

                for (AbstractInsnNode insn : block) {
                    newInsns.add(insn.clone(labelMap));
                }

                newInsns.add(endLabel);

            } else {
                for (AbstractInsnNode insn : block) {
                    newInsns.add(insn.clone(labelMap));
                }
            }
        }

        method.instructions.clear();
        method.instructions.add(newInsns);

        if (method.tryCatchBlocks != null && !method.tryCatchBlocks.isEmpty()) {
            List<TryCatchBlockNode> remapped = new ArrayList<>(method.tryCatchBlocks.size());

            for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
                LabelNode start = labelMap.get((LabelNode) tcb.start);
                LabelNode end = labelMap.get((LabelNode) tcb.end);
                LabelNode handler = labelMap.get((LabelNode) tcb.handler);

                if (start == null || end == null || handler == null) {
                    return;
                }

                remapped.add(new TryCatchBlockNode(start, end, handler, tcb.type));
            }

            method.tryCatchBlocks.clear();
            method.tryCatchBlocks.addAll(remapped);
        }
    }

    private List<List<AbstractInsnNode>> splitIntoBlocks(InsnList insns) {
        List<List<AbstractInsnNode>> blocks = new ArrayList<>();
        List<AbstractInsnNode> currentBlock = new ArrayList<>();

        for (AbstractInsnNode insn : insns.toArray()) {
            if (insn == null) continue;

            currentBlock.add(insn);

            if (insn instanceof JumpInsnNode ||
                    isReturn(insn) ||
                    insn instanceof TableSwitchInsnNode ||
                    insn instanceof LookupSwitchInsnNode) {

                blocks.add(new ArrayList<>(currentBlock));
                currentBlock.clear();
            }
        }

        if (!currentBlock.isEmpty()) blocks.add(currentBlock);
        return blocks;
    }

    private boolean shouldDuplicate(List<AbstractInsnNode> block) {
        if (block.size() < 2) return false;

        for (AbstractInsnNode insn : block) {
            if (insn instanceof LabelNode) return false;
            if (insn instanceof MethodInsnNode && insn.getOpcode() == Opcodes.INVOKESPECIAL) return false;
        }

        return true;
    }

    private boolean isReturn(AbstractInsnNode i) {
        return (i.getOpcode() >= 172 && i.getOpcode() <= 177) || i.getOpcode() == 191;
    }
}
