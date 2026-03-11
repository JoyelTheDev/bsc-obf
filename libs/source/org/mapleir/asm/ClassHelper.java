/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.objectweb.asm.ClassReader
 *  org.objectweb.asm.ClassVisitor
 *  org.objectweb.asm.ClassWriter
 *  org.objectweb.asm.tree.ClassNode
 */
package org.mapleir.asm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.mapleir.asm.ClassNode;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class ClassHelper {
    public static Collection<ClassNode> parseClasses(Class<?> ... a) throws IOException {
        ArrayList<ClassNode> list = new ArrayList<ClassNode>();
        for (int i = 0; i < a.length; ++i) {
            list.add(ClassHelper.create(a[i].getName()));
        }
        return list;
    }

    public static Map<String, ClassNode> convertToMap(Collection<ClassNode> classes) {
        HashMap<String, ClassNode> map = new HashMap<String, ClassNode>();
        for (ClassNode cn : classes) {
            map.put(cn.getName(), cn);
        }
        return map;
    }

    public static ClassNode create(byte[] bytes) {
        return ClassHelper.create(bytes, 6);
    }

    public static ClassNode create(byte[] bytes, int flags) {
        ClassReader reader = new ClassReader(bytes);
        org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
        reader.accept((ClassVisitor)node, flags);
        return new ClassNode(node);
    }

    public static ClassNode create(InputStream in, int flags) throws IOException {
        return ClassHelper.create(ClassHelper.readStream(in, true), flags);
    }

    public static ClassNode create(InputStream in) throws IOException {
        return ClassHelper.create(in, 6);
    }

    private static byte[] readStream(InputStream inputStream, boolean close) throws IOException {
        if (inputStream == null) {
            throw new IOException("Class not found");
        }
        try {
            byte[] byArray;
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();){
                int bytesRead;
                byte[] data = new byte[4096];
                while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                    outputStream.write(data, 0, bytesRead);
                }
                outputStream.flush();
                byArray = outputStream.toByteArray();
            }
            return byArray;
        }
        finally {
            if (close) {
                inputStream.close();
            }
        }
    }

    public static ClassNode create(org.objectweb.asm.tree.ClassNode cn) {
        return new ClassNode(cn);
    }

    public static ClassNode create(String name) throws IOException {
        return ClassHelper.create(name, 6);
    }

    public static ClassNode create(String name, int flags) throws IOException {
        return ClassHelper.create(ClassLoader.getSystemResourceAsStream(name.replace(".", "/") + ".class"), flags);
    }

    public static void dump(ClassNode cn, OutputStream outputStream) throws IOException {
        outputStream.write(ClassHelper.toByteArray(cn));
    }

    public static byte[] toByteArray(ClassNode cn) {
        return ClassHelper.toByteArray(cn, 2);
    }

    public static byte[] toByteArray(ClassNode cn, int flags) {
        ClassWriter writer = new ClassWriter(flags);
        cn.node.accept((ClassVisitor)writer);
        return writer.toByteArray();
    }
}

