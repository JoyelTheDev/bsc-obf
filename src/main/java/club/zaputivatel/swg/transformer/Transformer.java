package club.zaputivatel.swg.transformer;

import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;

public interface Transformer {
    void transform(ControlFlowGraph cfg, MethodNode method);
}
