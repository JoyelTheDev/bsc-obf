package club.zaputivatel.swg.transformer.impl;

import club.zaputivatel.swg.transformer.Transformer;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ControlFlowFlatteningMutator implements Transformer {

    @Override
    public void transform(ControlFlowGraph cfg, MethodNode mapleMethod) {
        org.objectweb.asm.tree.MethodNode method = mapleMethod.node;
        if (method.instructions == null || method.instructions.size() == 0) return;
        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) return;
        if ("<init>".equals(method.name) || "<clinit>".equals(method.name)) return;

        final Frame<BasicValue>[] frames;
        try {
            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicVerifier());
            frames = analyzer.analyze(
                    mapleMethod.owner != null ? mapleMethod.owner.getName() : "java/lang/Object",
                    method
            );
        } catch (Throwable t) {
            return;
        }

        AbstractInsnNode[] insns = method.instructions.toArray();
        if (insns.length == 0) return;

        int stateVar = method.maxLocals++;
        int xorKey = ThreadLocalRandom.current().nextInt(100_000, 900_000);

        LabelNode dispatcher = new LabelNode();
        LabelNode dispatcherDefault = new LabelNode();

        LabelNode realStart = new LabelNode();
        method.instructions.insertBefore(insns[0], realStart);

        Map<LabelNode, Integer> keys = new LinkedHashMap<>();
        Map<LabelNode, LabelNode> trampA = new HashMap<>();
        Map<LabelNode, LabelNode> trampB = new HashMap<>();

        int startKey = nextKey(keys.values());
        keys.put(realStart, startKey);

        for (int i = 0; i < insns.length; i++) {
            AbstractInsnNode insn = insns[i];
            if (!(insn instanceof LabelNode label)) continue;

            if (frames[i] == null) continue;
            if (frames[i].getStackSize() != 0) continue;

            if (!keys.containsKey(label)) {
                keys.put(label, nextKey(keys.values()));
            }
        }

        if (keys.size() < 2) return;

        for (LabelNode target : keys.keySet()) {
            trampA.put(target, new LabelNode());
            trampB.put(target, new LabelNode());
        }

        InsnList prologue = new InsnList();
        initializeLocals(method, frames, prologue);

        prologue.add(pushInt(startKey ^ xorKey));
        prologue.add(new VarInsnNode(Opcodes.ISTORE, stateVar));
        prologue.add(new JumpInsnNode(Opcodes.GOTO, dispatcher));

        method.instructions.insert(prologue);

        for (int i = 0; i < insns.length; i++) {
            AbstractInsnNode insn = insns[i];

            if (!(insn instanceof JumpInsnNode jmp)) continue;
            if (jmp.getOpcode() != Opcodes.GOTO) continue;
            if (frames[i] == null) continue;
            if (frames[i].getStackSize() != 0) continue;
            if (!keys.containsKey(jmp.label)) continue;

            int encoded = keys.get(jmp.label) ^ xorKey;

            InsnList repl = new InsnList();
            repl.add(pushInt(encoded));
            repl.add(new InsnNode(Opcodes.NOP));
            repl.add(new InsnNode(Opcodes.DUP));
            repl.add(new InsnNode(Opcodes.POP));
            repl.add(new VarInsnNode(Opcodes.ISTORE, stateVar));
            repl.add(new JumpInsnNode(Opcodes.GOTO, dispatcher));

            method.instructions.insertBefore(insn, repl);
            method.instructions.remove(insn);
        }

        InsnList dispatchList = new InsnList();
        dispatchList.add(dispatcher);

        List<Map.Entry<LabelNode, Integer>> entries = new ArrayList<>(keys.entrySet());
        Collections.shuffle(entries);

        for (Map.Entry<LabelNode, Integer> entry : entries) {
            LabelNode real = entry.getKey();
            LabelNode a = trampA.get(real);

            dispatchList.add(new VarInsnNode(Opcodes.ILOAD, stateVar));
            dispatchList.add(pushInt(xorKey));
            dispatchList.add(new InsnNode(Opcodes.IXOR));
            dispatchList.add(pushInt(entry.getValue()));
            dispatchList.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, a));
        }

        dispatchList.add(new JumpInsnNode(Opcodes.GOTO, dispatcherDefault));

        for (Map.Entry<LabelNode, Integer> entry : entries) {
            LabelNode real = entry.getKey();
            LabelNode a = trampA.get(real);
            LabelNode b = trampB.get(real);

            dispatchList.add(a);
            dispatchList.add(new InsnNode(Opcodes.NOP));
            dispatchList.add(new JumpInsnNode(Opcodes.GOTO, b));

            dispatchList.add(b);
            dispatchList.add(new InsnNode(Opcodes.NOP));
            dispatchList.add(new JumpInsnNode(Opcodes.GOTO, real));
        }

        LabelNode randomReal = entries.get(ThreadLocalRandom.current().nextInt(entries.size())).getKey();
        dispatchList.add(dispatcherDefault);
        dispatchList.add(new InsnNode(Opcodes.NOP));
        dispatchList.add(new JumpInsnNode(Opcodes.GOTO, trampA.get(randomReal)));

        method.instructions.add(dispatchList);

        insertFakeChains(method, dispatcher, entries, trampA);

        removeFrameNodes(method);
        method.localVariables = null;
        method.instructions.resetLabels();
    }

    private void insertFakeChains(org.objectweb.asm.tree.MethodNode method,
                                  LabelNode dispatcher,
                                  List<Map.Entry<LabelNode, Integer>> entries,
                                  Map<LabelNode, LabelNode> trampA) {
        int fakeChains = Math.max(3, entries.size() / 2);

        InsnList junk = new InsnList();

        for (int i = 0; i < fakeChains; i++) {
            LabelNode f1 = new LabelNode();
            LabelNode f2 = new LabelNode();
            LabelNode f3 = new LabelNode();

            LabelNode target = trampA.get(entries.get(ThreadLocalRandom.current().nextInt(entries.size())).getKey());

            junk.add(f1);
            junk.add(pushInt(ThreadLocalRandom.current().nextInt()));
            junk.add(new InsnNode(Opcodes.POP));
            junk.add(new JumpInsnNode(Opcodes.GOTO, f2));

            junk.add(f2);
            junk.add(new InsnNode(Opcodes.NOP));
            junk.add(new JumpInsnNode(Opcodes.GOTO, f3));

            junk.add(f3);
            junk.add(new InsnNode(Opcodes.NOP));
            junk.add(new JumpInsnNode(Opcodes.GOTO, target));
        }

        LabelNode entry = new LabelNode();
        InsnList entryJunk = new InsnList();
        entryJunk.add(entry);
        entryJunk.add(new JumpInsnNode(Opcodes.GOTO, dispatcher));
        junk.insert(entryJunk);

        method.instructions.add(junk);
    }

    private void initializeLocals(org.objectweb.asm.tree.MethodNode method,
                                  Frame<BasicValue>[] frames,
                                  InsnList prologue) {
        int originalMaxLocals = method.maxLocals - 1;

        int argsSize = ((method.access & Opcodes.ACC_STATIC) != 0) ? 0 : 1;
        for (Type t : Type.getArgumentTypes(method.desc)) {
            argsSize += t.getSize();
        }

        for (int i = argsSize; i < originalMaxLocals; i++) {
            Type type = null;

            for (Frame<BasicValue> frame : frames) {
                if (frame == null) continue;
                if (i >= frame.getLocals()) continue;

                BasicValue val = frame.getLocal(i);
                if (val == null) continue;
                if (val == BasicValue.UNINITIALIZED_VALUE) continue;
                if (val.getType() == null) continue;

                type = val.getType();
                break;
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

    private void removeFrameNodes(org.objectweb.asm.tree.MethodNode method) {
        for (AbstractInsnNode node : method.instructions.toArray()) {
            if (node instanceof FrameNode) {
                method.instructions.remove(node);
            }
        }
    }

    private int nextKey(Collection<Integer> used) {
        int k;
        do {
            k = ThreadLocalRandom.current().nextInt(1_000, 1_000_000);
        } while (used.contains(k));
        return k;
    }

    private AbstractInsnNode pushInt(int v) {
        if (v == -1) return new InsnNode(Opcodes.ICONST_M1);
        if (v == 0) return new InsnNode(Opcodes.ICONST_0);
        if (v == 1) return new InsnNode(Opcodes.ICONST_1);
        if (v == 2) return new InsnNode(Opcodes.ICONST_2);
        if (v == 3) return new InsnNode(Opcodes.ICONST_3);
        if (v == 4) return new InsnNode(Opcodes.ICONST_4);
        if (v == 5) return new InsnNode(Opcodes.ICONST_5);

        if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            return new IntInsnNode(Opcodes.BIPUSH, v);
        }

        if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            return new IntInsnNode(Opcodes.SIPUSH, v);
        }

        return new LdcInsnNode(v);
    }
}