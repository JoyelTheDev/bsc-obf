package club.zaputivatel.swg.ASM;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;


public class InstructionAddress {
    private final MethodNode _methodNode;
    private final AbstractInsnNode _abstractInsnNode;

    public InstructionAddress(MethodNode methodNode, AbstractInsnNode abstractInsnNode) {
        this._methodNode = methodNode;
        this._abstractInsnNode = abstractInsnNode;
    }
    public void remove(){
        _methodNode.instructions.remove(_abstractInsnNode);
    }
    public static InstructionAddress getInstructionAddress(MethodNode methodNode, AbstractInsnNode abstractInsnNode){
        return new InstructionAddress(methodNode,abstractInsnNode);
    }
}