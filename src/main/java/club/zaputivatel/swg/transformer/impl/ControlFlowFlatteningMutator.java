package club.zaputivatel.swg.transformer.impl;

import club.zaputivatel.swg.transformer.Transformer;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ControlFlowFlatteningMutator implements Transformer {

    @Override
    public void transform(ControlFlowGraph cfg, MethodNode mapleMethod) {
        org.objectweb.asm.tree.MethodNode method = mapleMethod.node;

        final Frame<BasicValue>[] frames;
        try {
            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicVerifier());
            frames = analyzer.analyze(mapleMethod.owner != null ? mapleMethod.owner.getName() : "java/lang/Object", method);
        } catch (Throwable t) {
            return;
        }

        int stateVar = method.maxLocals++;
        int xorKey = ThreadLocalRandom.current().nextInt(100000, 900000);

        LabelNode dispatcher = new LabelNode();
        LabelNode originalStart = new LabelNode();
        Map<LabelNode, Integer> keys = new HashMap<>();

        AbstractInsnNode[] insns = method.instructions.toArray();
        for (int i = 0; i < insns.length; i++) {
            if (insns[i] instanceof LabelNode label) {
                if (frames[i] != null && frames[i].getStackSize() == 0) {
                    keys.put(label, ThreadLocalRandom.current().nextInt(1000, 1000000));
                }
            }
        }

        if (keys.isEmpty()) return;

        InsnList prologue = new InsnList();
        initializeLocals(method, frames, prologue);

        int startKey = ThreadLocalRandom.current().nextInt(1000, 1000000);
        prologue.add(new LdcInsnNode(startKey ^ xorKey));
        prologue.add(new VarInsnNode(Opcodes.ISTORE, stateVar));
        prologue.add(new JumpInsnNode(Opcodes.GOTO, dispatcher));
        prologue.add(originalStart);
        keys.put(originalStart, startKey);

        for (int i = 0; i < insns.length; i++) {
            AbstractInsnNode insn = insns[i];
            if (insn.getOpcode() == Opcodes.GOTO && insn instanceof JumpInsnNode jmp) {
                if (keys.containsKey(jmp.label) && frames[i] != null && frames[i].getStackSize() == 0) {
                    InsnList repl = new InsnList();

                    int obfuscatedKey = keys.get(jmp.label) ^ xorKey;

                    repl.add(new LdcInsnNode(obfuscatedKey));
                    repl.add(new InsnNode(Opcodes.NOP));
                    repl.add(new InsnNode(Opcodes.DUP));
                    repl.add(new InsnNode(Opcodes.POP));

                    repl.add(new VarInsnNode(Opcodes.ISTORE, stateVar));
                    repl.add(new JumpInsnNode(Opcodes.GOTO, dispatcher));

                    method.instructions.insertBefore(insn, repl);
                    method.instructions.remove(insn);
                }
            }
        }

        List<Map.Entry<LabelNode, Integer>> entries = new ArrayList<>(keys.entrySet());
        entries.sort(Comparator.comparingInt(Map.Entry::getValue));

        int[] switchKeys = new int[entries.size()];
        LabelNode[] switchLabels = new LabelNode[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            switchKeys[i] = entries.get(i).getValue();
            switchLabels[i] = entries.get(i).getKey();
        }

        InsnList sw = new InsnList();
        sw.add(dispatcher);
        sw.add(new VarInsnNode(Opcodes.ILOAD, stateVar));

        sw.add(new LdcInsnNode(xorKey));
        sw.add(new InsnNode(Opcodes.IXOR));

        sw.add(new LookupSwitchInsnNode(originalStart, switchKeys, switchLabels));

        method.instructions.insert(prologue);
        method.instructions.add(sw);

        method.instructions.resetLabels();
        method.localVariables = null;
        method.instructions.iterator().forEachRemaining(node -> {
            if (node instanceof FrameNode) method.instructions.remove(node);
        });
    }

    private void initializeLocals(org.objectweb.asm.tree.MethodNode method, Frame<BasicValue>[] frames, InsnList prologue) {
        int argsSize = (method.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
        for (Type t : Type.getArgumentTypes(method.desc)) argsSize += t.getSize();

        for (int i = argsSize; i < method.maxLocals; i++) {
            Type type = null;
            for (Frame<BasicValue> frame : frames) {
                if (frame != null && i < frame.getLocals()) {
                    BasicValue val = frame.getLocal(i);
                    if (val != null && val.getType() != null && !val.equals(BasicValue.UNINITIALIZED_VALUE)) {
                        type = val.getType();
                        break;
                    }
                }
            }
            if (type == null) continue;

            switch (type.getSort()) {
                case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> {
                    prologue.add(new InsnNode(Opcodes.ICONST_0));
                    prologue.add(new VarInsnNode(Opcodes.ISTORE, i));
                }
                case Type.LONG -> {
                    prologue.add(new InsnNode(Opcodes.LCONST_0));
                    prologue.add(new VarInsnNode(Opcodes.LSTORE, i));
                }
                case Type.FLOAT -> {
                    prologue.add(new InsnNode(Opcodes.FCONST_0));
                    prologue.add(new VarInsnNode(Opcodes.FSTORE, i));
                }
                case Type.DOUBLE -> {
                    prologue.add(new InsnNode(Opcodes.DCONST_0));
                    prologue.add(new VarInsnNode(Opcodes.DSTORE, i));
                }
                case Type.ARRAY, Type.OBJECT -> {
                    prologue.add(new InsnNode(Opcodes.ACONST_NULL));
                    prologue.add(new VarInsnNode(Opcodes.ASTORE, i));
                }
            }
        }
    }
}