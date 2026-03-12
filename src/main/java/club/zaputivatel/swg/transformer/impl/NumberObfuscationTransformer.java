package club.zaputivatel.swg.transformer.impl;

import club.zaputivatel.swg.transformer.Transformer;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.concurrent.ThreadLocalRandom;

public class NumberObfuscationTransformer implements Transformer {

    @Override
    public void transform(ControlFlowGraph cfg, MethodNode mapleMethod) {
        org.objectweb.asm.tree.MethodNode method = mapleMethod.node;
        if (method.instructions == null || method.instructions.size() == 0) return;
        if (method.name.equals("<init>") || method.name.equals("<clinit>")) return;

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (!isIntegerConstant(insn)) continue;

            int value = getConstantValue(insn);
            method.instructions.insertBefore(insn, runner(value));
            method.instructions.remove(insn);
        }
    }

    private InsnList runner(int value) {
        return switch (ThreadLocalRandom.current().nextInt(5)) {
            case 0 -> mbaXorAddSub(value);
            case 1 -> mbaNotNeg(value);
            case 2 -> mbaSplit(value);
            case 3 -> mbaMasked(value);
            default -> mbaNested(value);
        };
    }


    private InsnList mbaXorAddSub(int value) {
        InsnList il = new InsnList();
        int a = rndNonZero();

        il.add(pushInt(value));
        il.add(pushInt(a));
        il.add(new InsnNode(Opcodes.IXOR));

        il.add(pushInt(2));
        il.add(pushInt(value));
        il.add(pushInt(a));
        il.add(new InsnNode(Opcodes.IAND));
        il.add(new InsnNode(Opcodes.IMUL));

        il.add(new InsnNode(Opcodes.IADD));
        il.add(pushInt(a));
        il.add(new InsnNode(Opcodes.ISUB));

        return il;
    }

    private InsnList mbaNotNeg(int value) {
        InsnList il = new InsnList();

        il.add(pushInt(value));
        il.add(new InsnNode(Opcodes.INEG));
        il.add(pushInt(1));
        il.add(new InsnNode(Opcodes.ISUB));
        il.add(new InsnNode(Opcodes.ICONST_M1));
        il.add(new InsnNode(Opcodes.IXOR));

        return il;
    }


    private InsnList mbaSplit(int value) {
        InsnList il = new InsnList();

        int p = ThreadLocalRandom.current().nextInt();
        int q = value - p;

        appendMbaIdentity(il, p);
        appendMbaIdentity(il, q);
        il.add(new InsnNode(Opcodes.IADD));

        return il;
    }


    private InsnList mbaMasked(int value) {
        InsnList il = new InsnList();
        int m = rndNonZero();

        appendMbaIdentity(il, value);
        pushIntoTempStyle(il, m, true);   // m
        il.add(new InsnNode(Opcodes.IXOR));

        il.add(pushInt(2));
        appendMbaIdentity(il, value);
        pushIntoTempStyle(il, m, false);
        il.add(new InsnNode(Opcodes.IAND));
        il.add(new InsnNode(Opcodes.IMUL));

        il.add(new InsnNode(Opcodes.IADD));
        pushIntoTempStyle(il, m, true);
        il.add(new InsnNode(Opcodes.ISUB));

        return il;
    }

    private InsnList mbaNested(int value) {
        InsnList il = new InsnList();
        int k = rndNonZero();

        appendMbaIdentity(il, value + k);
        pushIntoTempStyle(il, k, true);
        il.add(new InsnNode(Opcodes.ISUB));

        return il;
    }


    private void appendMbaIdentity(InsnList il, int x) {
        switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0 -> {
                int a = rndNonZero();
                il.add(pushInt(x));
                il.add(pushInt(a));
                il.add(new InsnNode(Opcodes.IXOR));

                il.add(pushInt(2));
                il.add(pushInt(x));
                il.add(pushInt(a));
                il.add(new InsnNode(Opcodes.IAND));
                il.add(new InsnNode(Opcodes.IMUL));

                il.add(new InsnNode(Opcodes.IADD));
                il.add(pushInt(a));
                il.add(new InsnNode(Opcodes.ISUB));
            }
            case 1 -> {
                il.add(pushInt(x));
                il.add(new InsnNode(Opcodes.ICONST_M1));
                il.add(new InsnNode(Opcodes.IXOR));
                il.add(new InsnNode(Opcodes.ICONST_M1));
                il.add(new InsnNode(Opcodes.IXOR));
            }
            case 2 -> {
                int a = ThreadLocalRandom.current().nextInt();
                int b = x - a;
                il.add(pushInt(a));
                il.add(pushInt(b));
                il.add(new InsnNode(Opcodes.IADD));
            }
            default -> {
                int a = ThreadLocalRandom.current().nextInt();
                int b = a ^ x;
                il.add(pushInt(a));
                il.add(pushInt(b));
                il.add(new InsnNode(Opcodes.IXOR));
            }
        }
    }


    private void pushIntoTempStyle(InsnList il, int x, boolean styleA) {
        if (styleA || ThreadLocalRandom.current().nextBoolean()) {
            il.add(pushInt(x));
        } else {
            il.add(pushInt(x));
            il.add(new InsnNode(Opcodes.ICONST_M1));
            il.add(new InsnNode(Opcodes.IXOR));
            il.add(new InsnNode(Opcodes.ICONST_M1));
            il.add(new InsnNode(Opcodes.IXOR));
        }
    }

    private boolean isIntegerConstant(AbstractInsnNode insn) {
        int op = insn.getOpcode();
        return (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5)
                || op == Opcodes.BIPUSH
                || op == Opcodes.SIPUSH
                || (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer);
    }

    private int getConstantValue(AbstractInsnNode insn) {
        int op = insn.getOpcode();

        if (op == Opcodes.ICONST_M1) return -1;
        if (op >= Opcodes.ICONST_0 && op <= Opcodes.ICONST_5) {
            return op - Opcodes.ICONST_0;
        }

        if (insn instanceof IntInsnNode ii) return ii.operand;
        if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer i) return i;

        throw new IllegalStateException("Not an int constant: " + insn);
    }

    private int rndNonZero() {
        int x;
        do {
            x = ThreadLocalRandom.current().nextInt();
        } while (x == 0);
        return x;
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