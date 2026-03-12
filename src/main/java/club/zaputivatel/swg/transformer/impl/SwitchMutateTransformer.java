package club.zaputivatel.swg.transformer.impl;

import club.zaputivatel.swg.transformer.Transformer;
import org.mapleir.asm.MethodNode;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SwitchMutateTransformer implements Transformer {

    @Override
    public void transform(ControlFlowGraph cfg, MethodNode mapleMethod) {
        org.objectweb.asm.tree.MethodNode method = mapleMethod.node;
        if (method.instructions == null) return;
        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) return;

        _convertTableSwitchToLookupSwitch(method);
        _obfuscateLookupSwitchKeys(method);

        method.localVariables = null;
        method.instructions.resetLabels();
    }

    private void _convertTableSwitchToLookupSwitch(org.objectweb.asm.tree.MethodNode method) {
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (!(insn instanceof TableSwitchInsnNode ts)) continue;

            int count = ts.labels.size();
            int[] keys = new int[count];
            for (int i = 0; i < count; i++) {
                keys[i] = ts.min + i;
            }

            LookupSwitchInsnNode ls = new LookupSwitchInsnNode(
                ts.dflt,
                keys,
                ts.labels.toArray(new LabelNode[0])
            );

            _sortLookupSwitch(ls);
            method.instructions.insertBefore(ts, ls);
            method.instructions.remove(ts);
        }
    }

    private void _obfuscateLookupSwitchKeys(org.objectweb.asm.tree.MethodNode method) {
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (!(insn instanceof LookupSwitchInsnNode ls)) continue;

            int xorKey = ThreadLocalRandom.current().nextInt();
            int addKey = ThreadLocalRandom.current().nextInt(1, 1_000_000);

            InsnList prefix = new InsnList();
            prefix.add(new LdcInsnNode(xorKey));
            prefix.add(new InsnNode(Opcodes.IXOR));
            prefix.add(new LdcInsnNode(addKey));
            prefix.add(new InsnNode(Opcodes.IADD));
            method.instructions.insertBefore(ls, prefix);

            for (int i = 0; i < ls.keys.size(); i++) {
                int k = ls.keys.get(i);
                int nk = (k ^ xorKey) + addKey;
                ls.keys.set(i, nk);
            }

            _sortLookupSwitch(ls);
        }
    }

    private void _sortLookupSwitch(LookupSwitchInsnNode node) {
        int n = node.keys.size();
        List<Entry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            entries.add(new Entry(node.keys.get(i), node.labels.get(i)));
        }

        entries.sort(Comparator.comparingInt(e -> e.key));

        node.keys.clear();
        node.labels.clear();
        for (Entry e : entries) {
            node.keys.add(e.key);
            node.labels.add(e.label);
        }
    }

    private static final class Entry {
        final int key;
        final LabelNode label;

        private Entry(int key, LabelNode label) {
            this.key = key;
            this.label = label;
        }
    }
}
