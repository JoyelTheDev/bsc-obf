package club.zaputivatel.swg.ASM;


import club.zaputivatel.swg.ASM.helper.ASMHelper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;


public class AdvancedClassNode extends ClassNode {
    public AdvancedClassNode() {
        this(589824);
        if (this.getClass() != AdvancedClassNode.class) {
            throw new IllegalStateException();
        }
    }

    private AdvancedClassNode(int api) {
        super(api);
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(this.access);
    }

    public boolean isInterface() {
        return Modifier.isInterface(this.access);
    }

    public boolean isAnnotation() {
        return (this.access & Opcodes.ACC_ANNOTATION) != 0;
    }

    public boolean isRecord() {
        return (this.access & Opcodes.ACC_RECORD) != 0;
    }

    public boolean isMixin() {
        if (this.invisibleAnnotations == null) return false;
        return this.invisibleAnnotations.stream().anyMatch(annotationNode -> annotationNode.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"));
    }

    public boolean isMain() {
        MethodNode main = ASMHelper.findMethod(this, "main", "([Ljava/lang/String;)V");
        return main == null || !Modifier.isPublic(main.access) || !Modifier.isStatic(main.access);
    }
    public MethodNode getMain() {
        if(!isMain()) return null;
        return ASMHelper.findMethod(this, "main", "([Ljava/lang/String;)V");
    }
    public static AdvancedClassNode createNode(byte[] bytecode, int parsingOptions) {
        AdvancedClassNode node = new AdvancedClassNode(Opcodes.ASM8);
        ClassReader reader = new ClassReader(bytecode);
        reader.accept(node, parsingOptions);
        return node;
    }

}
