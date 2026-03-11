/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.objectweb.asm.MethodVisitor
 *  org.objectweb.asm.tree.AbstractInsnNode
 *  org.objectweb.asm.tree.InsnList
 *  org.objectweb.asm.util.Printer
 *  org.objectweb.asm.util.Textifier
 *  org.objectweb.asm.util.TraceMethodVisitor
 */
package org.mapleir.asm;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

public class InsnListUtils {
    private static Printer printer = new Textifier();
    private static TraceMethodVisitor mp = new TraceMethodVisitor(printer);

    public static String insnListToString(InsnList insns) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < insns.size(); ++i) {
            sb.append(InsnListUtils.insnToString(insns.get(i)));
        }
        return sb.toString();
    }

    public static String insnToString(AbstractInsnNode insn) {
        insn.accept((MethodVisitor)mp);
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        return sw.toString();
    }
}

