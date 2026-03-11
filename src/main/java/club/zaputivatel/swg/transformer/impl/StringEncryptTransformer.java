package club.zaputivatel.swg.transformer.impl;

import club.zaputivatel.swg.transformer.Transformer;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

public class StringEncryptTransformer implements Transformer {

    @Override
    public void transform(ControlFlowGraph cfg, MethodNode mapleMethod) {
        org.objectweb.asm.tree.MethodNode method = mapleMethod.node;

        org.objectweb.asm.tree.ClassNode owner = mapleMethod.owner.node;

        if (method.instructions == null) return;

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
                if (s.isEmpty()) continue;

                String decryptorName = "z_" + _randomIdent(10);
                _createPolymorphicDecryptor(owner, decryptorName, s);

                method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, owner.name, decryptorName, "()Ljava/lang/String;", false));
            }
        }
    }

    private void _createPolymorphicDecryptor(org.objectweb.asm.tree.ClassNode classNode, String name, String value) {
        org.objectweb.asm.tree.MethodNode md = new org.objectweb.asm.tree.MethodNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                name,
                "()Ljava/lang/String;",
                null,
                null
        );
        InsnList il = md.instructions;
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        _emitInt(il, bytes.length);
        il.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE));
        il.add(new VarInsnNode(Opcodes.ASTORE, 0));

        for (int i = 0; i < bytes.length; i++) {
            il.add(new VarInsnNode(Opcodes.ALOAD, 0));
            _emitInt(il, i);

            int seed = ThreadLocalRandom.current().nextInt(10, 1000);
            int mode = ThreadLocalRandom.current().nextInt(3);

            switch (mode) {
                case 0:
                    _emitInt(il, bytes[i] ^ seed);
                    _emitInt(il, seed);
                    il.add(new InsnNode(Opcodes.IXOR));
                    break;
                case 1:
                    _emitInt(il, bytes[i] + seed);
                    _emitInt(il, seed);
                    il.add(new InsnNode(Opcodes.ISUB));
                    break;
                default:
                    int multi = ThreadLocalRandom.current().nextInt(2, 5);
                    _emitInt(il, (bytes[i] ^ seed) * multi);
                    _emitInt(il, multi);
                    il.add(new InsnNode(Opcodes.IDIV));
                    _emitInt(il, seed);
                    il.add(new InsnNode(Opcodes.IXOR));
                    break;
            }

            il.add(new InsnNode(Opcodes.I2B));
            il.add(new InsnNode(Opcodes.BASTORE));
        }

        il.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_8", "Ljava/nio/charset/Charset;"));
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V", false));
        il.add(new InsnNode(Opcodes.ARETURN));

        md.maxStack = 6;
        md.maxLocals = 2;
        classNode.methods.add(md);
    }

    private void _emitInt(InsnList il, int value) {
        if (value >= -1 && value <= 5) {
            il.add(new InsnNode(Opcodes.ICONST_0 + value));
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            il.add(new IntInsnNode(Opcodes.BIPUSH, value));
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            il.add(new IntInsnNode(Opcodes.SIPUSH, value));
        } else {
            il.add(new LdcInsnNode(value));
        }
    }

    private String _randomIdent(int len) {
        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}