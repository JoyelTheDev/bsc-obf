package club.zaputivatel.swg.ASM.helper;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class ASMHelper implements Opcodes {

    public static MethodNode findMethod(ClassNode classNode, String name, String desc) {
        return classNode.methods
                .stream()
                .filter(methodNode -> name.equals(methodNode.name) && desc.equals(methodNode.desc))
                .findAny()
                .orElse(null);
    }

    public static boolean isString(AbstractInsnNode node) {
        return node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof String;
    }

    public static boolean isType(AbstractInsnNode node) {
        return node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof Type;
    }

    public static String getString(AbstractInsnNode node) {
        return (String) ((LdcInsnNode) node).cst;
    }

    public static Type getType(AbstractInsnNode node) {
        return (Type) ((LdcInsnNode) node).cst;
    }

    public static Integer getIntegerOrNull(AbstractInsnNode node) {
        if (node.getOpcode() >= ICONST_M1 && node.getOpcode() <= ICONST_5) {
            return node.getOpcode() - ICONST_0; // Получение значения по индексу.Они упорядочены.
        } else if (node.getOpcode() == SIPUSH || node.getOpcode() == BIPUSH) {
            return ((IntInsnNode) node).operand;
        } else if (node instanceof LdcInsnNode && ((LdcInsnNode) node).cst instanceof Integer) {
            return (int) ((LdcInsnNode) node).cst;
        }
        return null;
    }

    public static Long getLongOrNull(AbstractInsnNode node) {

        if (node.getOpcode() >= LCONST_0 && node.getOpcode() <= LCONST_1) {
            return (long) (node.getOpcode() - LCONST_0);
        } else if (node.getOpcode() == LDC) {
            if (node instanceof LdcInsnNode) {
                if (((LdcInsnNode) node).cst instanceof Long) {
                    return (long) ((LdcInsnNode) node).cst;
                }
            }
        }
        return null;
    }

    public static Float getFloatOrNull(AbstractInsnNode node) {
        if (node.getOpcode() >= FCONST_0 && node.getOpcode() <= FCONST_2) {
            return (float) (node.getOpcode() - FCONST_0);
        } else if (node instanceof LdcInsnNode) {
            if (((LdcInsnNode) node).cst instanceof Float) {
                return (Float) ((LdcInsnNode) node).cst;
            }
        }
        return null;
    }

    public static Double getDoubleOrNull(AbstractInsnNode node) {

        if (node.getOpcode() >= DCONST_0 && node.getOpcode() <= DCONST_1) {
            return (double) (node.getOpcode() - DCONST_0);
        } else {
            if (node instanceof LdcInsnNode) {
                if (((LdcInsnNode) node).cst instanceof Double) {
                    return (Double) ((LdcInsnNode) node).cst;
                }
            }


        }

        return null;

    }

    public static boolean isClass(String fileName, byte[] bytes) {
        return bytes.length >= 4 && String
                .format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]).equals("CAFEBABE") && (
                fileName.endsWith(".class") || fileName.endsWith(".class/"));
    }

 
}
