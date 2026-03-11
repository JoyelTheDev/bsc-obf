package club.zaputivatel.swg.mapleIR;

import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.BasicBlock;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.builder.ControlFlowGraphBuilder;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.stmt.copy.CopyVarStmt;
import org.mapleir.ir.locals.LocalsPool;

import java.util.Iterator;

public class CFGParser {

    public static void buildCFGAndExport(org.objectweb.asm.tree.MethodNode methodNodeASM, org.objectweb.asm.tree.ClassNode classNodeASM) {

        MethodNode methodNode = ASM2MAPLER.methodNode2Map(methodNodeASM, classNodeASM);
        ControlFlowGraph controlFlowGraph = ControlFlowGraphBuilder.build(methodNode);
        LocalsPool localsPool = controlFlowGraph.getLocals();
        System.out.println(classNodeASM.name + " " + methodNodeASM.name + " " + methodNode.getDesc());

//        if (localsPool.getMaxStack() != 0) {
//            for (Local local : localsPool.getCache().values()){
//                if (local.getType() == null)continue;
//                System.out.println(local.getType() + " " + local + " " );
//            }
//        }
        for (BasicBlock b : controlFlowGraph.verticesInOrder()) {
            System.out.println(b.getDisplayName()+ ": ");
            for (Iterator<Stmt> it = b.stream().iterator(); it.hasNext(); ) {
                Stmt stmt = it.next();
                if (stmt instanceof CopyVarStmt copyVarStmt) {
                    System.out.println(copyVarStmt);
                }

                else {
                    // System.out.println(stmt.getClass());
                    System.out.println(stmt);
                }

            }
        }
        System.out.println();

    }
}
