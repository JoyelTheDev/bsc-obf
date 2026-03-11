package club.zaputivatel.swg.mapleIR;

import org.mapleir.asm.ClassHelper;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;

/**
 * powered by dmkn
 * просто получаем байткод
 */
public class ASM2MAPLER {

    public static MethodNode methodNode2Map(org.objectweb.asm.tree.MethodNode methodNode, org.objectweb.asm.tree.ClassNode classNode) {
        return new MethodNode(methodNode, convertClass(classNode));
    }

    public static ClassNode convertClass(org.objectweb.asm.tree.ClassNode classNode) {
        return ClassHelper.create(classNode);
    }

}