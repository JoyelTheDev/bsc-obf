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
        if (method.instructions == null) return;
        if (method.name.equals("<init>")) return;

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (isIntegerConstant(insn)) {
                long value = getConstantValue(insn);

                method.instructions.insertBefore(insn, obfuscateAgressive(value));
                method.instructions.remove(insn);
            }
        }
    }

    private InsnList obfuscateAgressive(long value) {
        InsnList il = new InsnList();
        int x = ThreadLocalRandom.current().nextInt(100, 1000);
        int y = ThreadLocalRandom.current().nextInt(100, 1000);
        int key = ThreadLocalRandom.current().nextInt(100, 1000);


        il.add(new InsnNode(Opcodes.ICONST_2));
        il.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_INT));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new LdcInsnNode((int)value ^ key));
        il.add(new InsnNode(Opcodes.IASTORE));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new InsnNode(Opcodes.ICONST_1));
        il.add(new LdcInsnNode(key));
        il.add(new InsnNode(Opcodes.IASTORE));

        il.add(new InsnNode(Opcodes.DUP));
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new InsnNode(Opcodes.IALOAD));
        il.add(new InsnNode(Opcodes.SWAP));
        il.add(new InsnNode(Opcodes.ICONST_1));
        il.add(new InsnNode(Opcodes.IALOAD));
        il.add(new InsnNode(Opcodes.IXOR));

        il.add(new LdcInsnNode(x));
        il.add(new LdcInsnNode(y));
        il.add(new InsnNode(Opcodes.IXOR)); // (x^y)

        il.add(new InsnNode(Opcodes.ICONST_2));
        il.add(new LdcInsnNode(x));
        il.add(new LdcInsnNode(y));
        il.add(new InsnNode(Opcodes.IAND));
        il.add(new InsnNode(Opcodes.IMUL)); // 2*(x&y)

        il.add(new InsnNode(Opcodes.IADD)); // (x^y) + 2*(x&y) == (x+y)

        il.add(new LdcInsnNode(x));
        il.add(new LdcInsnNode(y));
        il.add(new InsnNode(Opcodes.IADD)); // (x+y)

        il.add(new InsnNode(Opcodes.ISUB));
        il.add(new InsnNode(Opcodes.IADD));

        return il;
    }

    private boolean isIntegerConstant(AbstractInsnNode insn) {
        int op = insn.getOpcode();
        return (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) ||
                op == Opcodes.BIPUSH || op == Opcodes.SIPUSH ||
                (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer);
    }

    private long getConstantValue(AbstractInsnNode insn) {
        int op = insn.getOpcode();
        if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) return op - Opcodes.ICONST_0;
        if (insn instanceof IntInsnNode ii) return ii.operand;
        if (insn instanceof LdcInsnNode ldc) return ((Number) ldc.cst).longValue();
        return 0;
    }
}