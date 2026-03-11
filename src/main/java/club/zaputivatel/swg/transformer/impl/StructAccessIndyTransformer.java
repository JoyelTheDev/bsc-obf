package club.zaputivatel.swg.transformer.impl;

import club.zaputivatel.swg.transformer.Transformer;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StructAccessIndyTransformer implements Transformer {
    private static final Random RANDOM = new Random();

    private final String BSM_NAME = "λ_" + Integer.toHexString(RANDOM.nextInt(0xFFFF));
    private final String XOR_NAME = "ξ_" + Integer.toHexString(RANDOM.nextInt(0xFFFF));

    private final String BSM_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;I)Ljava/lang/invoke/CallSite;";

    @Override
    public void transform(ControlFlowGraph cfg, MethodNode mapleMethod) {
        org.objectweb.asm.tree.MethodNode method = mapleMethod.node;
        if (method == null || method.instructions == null) return;

        org.objectweb.asm.tree.ClassNode ownerClass = mapleMethod.owner.node;
        int classHash = ownerClass.name.hashCode();
        int baseKey = 0xAA;

        boolean changed = false;

        List<PendingIndy> pending = new ArrayList<>();
        //cring
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof FieldInsnNode fin) {
                if (fin.name.equals(BSM_NAME) || fin.name.equals(XOR_NAME)) continue;

                String indyDesc = getFieldIndyDesc(fin);

                String payload = fin.owner.replace('/', '.') + "\u0000" + fin.name + "\u0000" + fin.desc;
                int salt = RANDOM.nextInt(0xFFFF);
                int key = baseKey ^ classHash ^ salt;
                pending.add(new PendingIndy(fin, indyDesc, xorString(payload, key), salt, fin.getOpcode()));
            } else if (insn instanceof MethodInsnNode min) {
                if (min.name.equals("<init>") || min.name.equals(BSM_NAME) || min.name.equals(XOR_NAME)) continue;

                String indyDesc = (min.getOpcode() == Opcodes.INVOKESTATIC)
                        ? min.desc
                        : min.desc.replace("(", "(L" + min.owner + ";");

                String payload = min.owner.replace('/', '.') + "\u0000" + min.name + "\u0000" + min.desc;
                int salt = RANDOM.nextInt(0xFFFF);
                int key = baseKey ^ classHash ^ salt;
                pending.add(new PendingIndy(min, indyDesc, xorString(payload, key), salt, min.getOpcode()));
            }
        }

        if (!pending.isEmpty()) {
            ensureBootstrap(ownerClass);

            Handle bsmHandle = new Handle(Opcodes.H_INVOKESTATIC, ownerClass.name, BSM_NAME, BSM_DESC, false);
            for (PendingIndy p : pending) {
                method.instructions.set(p.target, new InvokeDynamicInsnNode(
                        "_", p.indyDesc, bsmHandle,
                        p.encryptedPayload,
                        Integer.valueOf(p.salt),
                        Integer.valueOf(classHash),
                        p.opcode
                ));
                changed = true;
            }
        }

        if (changed) {
            ownerClass.version = Math.max(Opcodes.V1_7, ownerClass.version);
            method.localVariables = null;
        }
    }

    private String getFieldIndyDesc(FieldInsnNode fin) {
        switch (fin.getOpcode()) {
            case Opcodes.GETSTATIC: return "()" + fin.desc;
            case Opcodes.GETFIELD:  return "(L" + fin.owner + ";)" + fin.desc;
            case Opcodes.PUTSTATIC: return "(" + fin.desc + ")V";
            case Opcodes.PUTFIELD:  return "(L" + fin.owner + ";" + fin.desc + ")V";
            default: return fin.desc;
        }
    }

    private void ensureBootstrap(org.objectweb.asm.tree.ClassNode owner) {
        if (owner.methods.stream().anyMatch(m -> m.name.equals(BSM_NAME))) return;

        injectXorMethod(owner);

        org.objectweb.asm.tree.MethodNode bsm = new org.objectweb.asm.tree.MethodNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                BSM_NAME, BSM_DESC, null, null);

        InsnList il = bsm.instructions;

        il.add(pushInt(0xAA));
        il.add(new VarInsnNode(Opcodes.ALOAD, 4));
        il.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Integer"));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
        il.add(new InsnNode(Opcodes.IXOR));
        il.add(new VarInsnNode(Opcodes.ALOAD, 5));
        il.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Integer"));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
        il.add(new InsnNode(Opcodes.IXOR));
        il.add(new VarInsnNode(Opcodes.ISTORE, 12));

        il.add(new VarInsnNode(Opcodes.ALOAD, 3));
        il.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/String"));
        il.add(new VarInsnNode(Opcodes.ILOAD, 12));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner.name, XOR_NAME, "(Ljava/lang/String;I)Ljava/lang/String;", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, 3));

        il.add(new VarInsnNode(Opcodes.ALOAD, 3));
        il.add(new LdcInsnNode("\u0000"));
        il.add(pushInt(3));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "split", "(Ljava/lang/String;I)[Ljava/lang/String;", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, 11));

        il.add(new VarInsnNode(Opcodes.ALOAD, 11));
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new InsnNode(Opcodes.AALOAD));
        il.add(new VarInsnNode(Opcodes.ASTORE, 3));
        il.add(new VarInsnNode(Opcodes.ALOAD, 11));
        il.add(new InsnNode(Opcodes.ICONST_1));
        il.add(new InsnNode(Opcodes.AALOAD));
        il.add(new VarInsnNode(Opcodes.ASTORE, 4));
        il.add(new VarInsnNode(Opcodes.ALOAD, 11));
        il.add(new InsnNode(Opcodes.ICONST_2));
        il.add(new InsnNode(Opcodes.AALOAD));
        il.add(new VarInsnNode(Opcodes.ASTORE, 5));

        il.add(new VarInsnNode(Opcodes.ALOAD, 3));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, 7));

        LabelNode lMethod = new LabelNode();
        il.add(new VarInsnNode(Opcodes.ILOAD, 6));
        il.add(pushInt(181));
        il.add(new JumpInsnNode(Opcodes.IF_ICMPGT, lMethod));

        il.add(new VarInsnNode(Opcodes.ALOAD, 7)); // owner class
        il.add(new VarInsnNode(Opcodes.ALOAD, 4)); // field name
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", "getType", "()Ljava/lang/Class;", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, 8)); // Field type Class

        LabelNode f1 = new LabelNode(), f2 = new LabelNode(), f3 = new LabelNode(), f4 = new LabelNode(), fEnd = new LabelNode();
        il.add(new VarInsnNode(Opcodes.ILOAD, 6));
        il.add(pushInt(178)); il.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, f1)); // GETSTATIC
        il.add(new VarInsnNode(Opcodes.ILOAD, 6));
        il.add(pushInt(179)); il.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, f2)); // PUTSTATIC
        il.add(new VarInsnNode(Opcodes.ILOAD, 6));
        il.add(pushInt(180)); il.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, f3)); // GETFIELD
        il.add(new JumpInsnNode(Opcodes.GOTO, f4));
        
        il.add(f1);
        il.add(new VarInsnNode(Opcodes.ALOAD, 8)); // rtype
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType", "methodType", "(Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", false));
        il.add(new JumpInsnNode(Opcodes.GOTO, fEnd));
        
        il.add(f2);
        il.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;")); // rtype
        il.add(new VarInsnNode(Opcodes.ALOAD, 8)); // ptype0
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType", "methodType", "(Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", false));
        il.add(new JumpInsnNode(Opcodes.GOTO, fEnd));
        
        il.add(f3);
        il.add(new VarInsnNode(Opcodes.ALOAD, 8)); // rtype
        il.add(new VarInsnNode(Opcodes.ALOAD, 7)); // ptype0 (owner)
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType", "methodType", "(Ljava/lang/Class;Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", false));
        il.add(new JumpInsnNode(Opcodes.GOTO, fEnd));
        
        il.add(f4);
        il.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;")); // rtype
        il.add(new InsnNode(Opcodes.ICONST_2)); // ptypes count
        il.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Class"));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new VarInsnNode(Opcodes.ALOAD, 7)); // owner
        il.add(new InsnNode(Opcodes.AASTORE));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new InsnNode(Opcodes.ICONST_1));
        il.add(new VarInsnNode(Opcodes.ALOAD, 8)); // field type
        il.add(new InsnNode(Opcodes.AASTORE));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType", "methodType", "(Ljava/lang/Class;[Ljava/lang/Class;)Ljava/lang/invoke/MethodType;", false));
        
        il.add(fEnd);
        il.add(new VarInsnNode(Opcodes.ASTORE, 10)); // CallSite MethodType

        //findGetter/findSetter/findStaticGetter/findStaticSetter
        il.add(new VarInsnNode(Opcodes.ALOAD, 0)); // lookup
        il.add(new VarInsnNode(Opcodes.ALOAD, 7)); // owner class
        il.add(new VarInsnNode(Opcodes.ALOAD, 4)); // name
        il.add(new VarInsnNode(Opcodes.ALOAD, 8)); // field type class

        LabelNode h1 = new LabelNode(), h2 = new LabelNode(), h3 = new LabelNode(), hEnd = new LabelNode();
        il.add(new VarInsnNode(Opcodes.ILOAD, 6));
        il.add(pushInt(178)); il.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, h1)); // GETSTATIC
        il.add(new VarInsnNode(Opcodes.ILOAD, 6));
        il.add(pushInt(179)); il.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, h2)); // PUTSTATIC
        il.add(new VarInsnNode(Opcodes.ILOAD, 6));
        il.add(pushInt(180)); il.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, h3)); // GETFIELD
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
        il.add(new JumpInsnNode(Opcodes.GOTO, hEnd));
        il.add(h1); il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStaticGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false)); il.add(new JumpInsnNode(Opcodes.GOTO, hEnd));
        il.add(h2); il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStaticSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false)); il.add(new JumpInsnNode(Opcodes.GOTO, hEnd));
        il.add(h3); il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;", false));
        il.add(hEnd);
        il.add(new VarInsnNode(Opcodes.ASTORE, 9)); // MethodHandle
        
        il.add(new VarInsnNode(Opcodes.ALOAD, 9)); // mh
        il.add(new VarInsnNode(Opcodes.ALOAD, 10)); // callSite methodType
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, 9));
        il.add(new JumpInsnNode(Opcodes.GOTO, createCallSite(il)));

        il.add(lMethod);
        il.add(new VarInsnNode(Opcodes.ALOAD, 5)); // desc
        il.add(new VarInsnNode(Opcodes.ALOAD, 7));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false));
        il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, 8)); // MethodType

        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new VarInsnNode(Opcodes.ALOAD, 7));
        il.add(new VarInsnNode(Opcodes.ALOAD, 4));
        il.add(new VarInsnNode(Opcodes.ALOAD, 8));

        LabelNode m1 = new LabelNode(), mEnd = new LabelNode();
        il.add(new VarInsnNode(Opcodes.ILOAD, 6));
        il.add(pushInt(184)); // INVOKESTATIC > ALL
        il.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, m1));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
        il.add(new JumpInsnNode(Opcodes.GOTO, mEnd));
        il.add(m1);
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandles$Lookup", "findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
        il.add(mEnd);
        il.add(new VarInsnNode(Opcodes.ASTORE, 9));

        createCallSite(il);

        owner.methods.add(bsm);
    }

    private LabelNode createCallSite(InsnList il) {
        LabelNode label = new LabelNode();
        il.add(label);
        il.add(new TypeInsnNode(Opcodes.NEW, "java/lang/invoke/ConstantCallSite"));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new VarInsnNode(Opcodes.ALOAD, 9)); //MethodHandle
        il.add(new VarInsnNode(Opcodes.ALOAD, 2)); //target MethodType
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false));
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/invoke/ConstantCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false));
        il.add(new InsnNode(Opcodes.ARETURN));
        return label;
    }

    private void injectXorMethod(org.objectweb.asm.tree.ClassNode owner) {
        if (owner.methods.stream().anyMatch(m -> m.name.equals(XOR_NAME))) return;
        org.objectweb.asm.tree.MethodNode m = new org.objectweb.asm.tree.MethodNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                XOR_NAME, "(Ljava/lang/String;I)Ljava/lang/String;", null, null);

        InsnList il = m.instructions;
        LabelNode loop = new LabelNode(), end = new LabelNode();
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, 2));
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new VarInsnNode(Opcodes.ISTORE, 3));
        il.add(loop);
        il.add(new VarInsnNode(Opcodes.ILOAD, 3));
        il.add(new VarInsnNode(Opcodes.ALOAD, 2));
        il.add(new InsnNode(Opcodes.ARRAYLENGTH));
        il.add(new JumpInsnNode(Opcodes.IF_ICMPGE, end));
        il.add(new VarInsnNode(Opcodes.ALOAD, 2));
        il.add(new VarInsnNode(Opcodes.ILOAD, 3));
        il.add(new InsnNode(Opcodes.DUP2));
        il.add(new InsnNode(Opcodes.CALOAD));
        il.add(new VarInsnNode(Opcodes.ILOAD, 1));
        il.add(new InsnNode(Opcodes.IXOR));
        il.add(new InsnNode(Opcodes.I2C));
        il.add(new InsnNode(Opcodes.CASTORE));
        il.add(new IincInsnNode(3, 1));
        il.add(new JumpInsnNode(Opcodes.GOTO, loop));
        il.add(end);
        il.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new VarInsnNode(Opcodes.ALOAD, 2));
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([C)V", false));
        il.add(new InsnNode(Opcodes.ARETURN));
        owner.methods.add(m);
    }

    private static final class PendingIndy {
        private final AbstractInsnNode target;
        private final String indyDesc;
        private final String encryptedPayload;
        private final int salt;
        private final int opcode;

        private PendingIndy(AbstractInsnNode target, String indyDesc, String encryptedPayload, int salt, int opcode) {
            this.target = target;
            this.indyDesc = indyDesc;
            this.encryptedPayload = encryptedPayload;
            this.salt = salt;
            this.opcode = opcode;
        }
    }

    private String xorString(String s, int key) {
        char[] res = s.toCharArray();
        for (int i = 0; i < res.length; i++) res[i] ^= (char)key;
        return new String(res);
    }

    private AbstractInsnNode pushInt(int value) {
        if (value >= -1 && value <= 5) return new InsnNode(Opcodes.ICONST_0 + value);
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) return new IntInsnNode(Opcodes.BIPUSH, value);
        return new IntInsnNode(Opcodes.SIPUSH, value);
    }
}