package club.zaputivatel.swg.transformer.impl;

import club.zaputivatel.swg.transformer.Transformer;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Random;

public class CrasherTransformer implements Transformer {

    private static final int VIRTUAL_CALL_COUNT = 3500;
    private static final Random random = new Random();

    @Override
    public void transform(ControlFlowGraph cfg, MethodNode mapleMethod) {

        ClassNode classNode = mapleMethod.owner.node;

        addRandomFields(classNode);
        addTrashMethods(classNode);
    }

    private void addTrashMethods(ClassNode classNode) {

        for (int i = 0; i < 80; i++) {

            String name = randomName();

            org.objectweb.asm.tree.MethodNode method =
                    new org.objectweb.asm.tree.MethodNode(
                            Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                            name,
                            "()V",
                            null,
                            null
                    );

            InsnList insn = new InsnList();

            insn.add(new TypeInsnNode(Opcodes.NEW, "java/lang/Object"));
            insn.add(new InsnNode(Opcodes.DUP));
            insn.add(new MethodInsnNode(
                    Opcodes.INVOKESPECIAL,
                    "java/lang/Object",
                    "<init>",
                    "()V",
                    false
            ));

            for (int j = 0; j < VIRTUAL_CALL_COUNT; j++) {

                insn.add(new InsnNode(Opcodes.DUP));

                insn.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/lang/Object",
                        "toString",
                        "()Ljava/lang/String;",
                        false
                ));

                insn.add(new InsnNode(Opcodes.POP));
            }

            insn.add(new InsnNode(Opcodes.RETURN));

            method.instructions.add(insn);

            classNode.methods.add(method);
        }
    }

    private void addRandomFields(ClassNode classNode) {

        for (int i = 0; i < 15; i++) {

            FieldNode field = new FieldNode(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC,
                    randomName(),
                    "Ljava/lang/String;",
                    null,
                    null
            );

            classNode.fields.add(field);
        }
    }

    private String randomName() {

        StringBuilder sb = new StringBuilder();

        sb.append("\u2060");
        sb.append((char)(random.nextInt(2000) + 500));
        sb.append(random.nextInt(9999));
        sb.append("\u0378");

        return sb.toString();
    }
}