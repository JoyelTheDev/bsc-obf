package club.zaputivatel.swg;

import club.zaputivatel.swg.ASM.FuckyClassWriter;
import club.zaputivatel.swg.ASM.helper.ASMHelper;
import club.zaputivatel.swg.mapleIR.ASM2MAPLER;
import club.zaputivatel.swg.transformer.Transformer;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.builder.ControlFlowGraphBuilder;
import org.mapleir.ir.codegen.ControlFlowGraphDumper;
import org.mapleir.ir.algorithms.BoissinotDestructor;
import org.mapleir.ir.algorithms.LocalsReallocator;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ObfuscatorCore {
    private final Path in;
    private final Path out;
    private final int classReaderFlags;
    private final int classWriterFlags;
    private final boolean debug;

    private final List<org.objectweb.asm.tree.ClassNode> asmClassNodes = new ArrayList<>();
    private final Map<String, byte[]> resources = new LinkedHashMap<>();
    private final List<Transformer> transformers;

    private ObfuscatorCore(Builder builder) {
        this.in = builder.in;
        this.out = builder.out;
        this.classReaderFlags = builder.classReaderFlags;
        this.classWriterFlags = builder.classWriterFlags;
        this.debug = builder.debug;
        this.transformers = builder.transformers;

        if (!Files.exists(in)) {
            throw new IllegalArgumentException("Input file not found: " + in.toAbsolutePath());
        }
    }

    public void start() {
        load();
        process();
        save();
    }

    private void load() {
        if (debug) {
            System.out.println("Loading jar: " + in.toAbsolutePath());
        }

        Map<String, byte[]> files = loadFilesFromZip(in);
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            String name = e.getKey();
            byte[] data = e.getValue();

            if (ASMHelper.isClass(name, data)) {
                org.objectweb.asm.tree.ClassNode asmNode = new org.objectweb.asm.tree.ClassNode();
                org.objectweb.asm.ClassReader reader = new org.objectweb.asm.ClassReader(data);
                reader.accept(asmNode, classReaderFlags);
                asmClassNodes.add(asmNode);
            } else {
                resources.put(name, data);
            }
        }

        if (debug) {
            System.out.println("Loaded classes: " + asmClassNodes.size());
            System.out.println("Loaded resources: " + resources.size());
        }
    }

    private void process() {
        for (org.objectweb.asm.tree.ClassNode asmNode : asmClassNodes) {
            if (debug) {
                System.out.println("Processing ASM class: " + asmNode.name);
            }

            org.mapleir.asm.ClassNode mapleClass = ASM2MAPLER.convertClass(asmNode);

            @SuppressWarnings("unchecked")
            List<org.objectweb.asm.tree.MethodNode> methods = new ArrayList<>(asmNode.methods);
            for (org.objectweb.asm.tree.MethodNode m : methods) {
                if ((m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) continue;

                org.objectweb.asm.tree.MethodNode originalMethod = new org.objectweb.asm.tree.MethodNode(
                    m.access, m.name, m.desc, m.signature, m.exceptions.toArray(new String[0])
                );
                m.accept(originalMethod);

                org.mapleir.asm.MethodNode mapleMethod = new org.mapleir.asm.MethodNode(m, mapleClass);
                ControlFlowGraph cfg;
                try {
                    cfg = ControlFlowGraphBuilder.build(mapleMethod);
                } catch (Throwable t) {
                    if (debug) {
                        System.err.println("Failed to build CFG: " + asmNode.name + "." + m.name + m.desc);
                        t.printStackTrace();
                    }
                    continue;
                }

                boolean transformerFailed = false;
                for (Transformer t : transformers) {
                    try {
                        t.transform(cfg, mapleMethod);
                    } catch (Throwable ex) {
                        if (debug) {
                            System.err.println("Transformer failed: " + t.getClass().getSimpleName() + " on " + asmNode.name + "." + m.name + m.desc);
                            ex.printStackTrace();
                        }
                        transformerFailed = true;
                        break;
                    }
                }

                if (transformerFailed) {
                    m.instructions.clear();
                    m.tryCatchBlocks.clear();
                    m.localVariables = null;
                    originalMethod.accept(m);
                    continue;
                }

                try {
                    BoissinotDestructor.leaveSSA(cfg);
                    LocalsReallocator.realloc(cfg);
                } catch (Throwable t) {
                    if (debug) {
                        System.err.println("BoissinotDestructor failed: " + asmNode.name + "." + m.name + m.desc);
                        t.printStackTrace();
                    }
                    m.instructions.clear();
                    m.tryCatchBlocks.clear();
                    m.localVariables = null;
                    originalMethod.accept(m);
                    continue;
                }

                boolean hasAsmTransformer = false;
                for (Transformer t : transformers) {
                    if (t.getClass().getSimpleName().contains("Mutator")) {
                        hasAsmTransformer = true;
                        break;
                    }
                }

                if (!hasAsmTransformer) {
                    try {
                        new ControlFlowGraphDumper(cfg, mapleMethod).dump();
                    } catch (Throwable t) {
                        if (debug) {
                            System.err.println("Failed to dump CFG: " + asmNode.name + "." + m.name + m.desc);
                            t.printStackTrace();
                        }
                        m.instructions.clear();
                        m.tryCatchBlocks.clear();
                        m.localVariables = null;
                        originalMethod.accept(m);
                        continue;
                    }
                }

                try {
                    cfg.verify();
                } catch (Throwable t) {
                    if (debug) {
                        System.err.println("CFG verification failed, restoring method: " + asmNode.name + "." + m.name + m.desc);
                        t.printStackTrace();
                    }
                    m.instructions.clear();
                    m.tryCatchBlocks.clear();
                    m.localVariables = null;
                    originalMethod.accept(m);
                    continue;
                }
            }
        }
    }

    private void save() {
        if (debug) {
            System.out.println("Saving jar: " + out.toAbsolutePath());
        }

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(out))) {
            for (int i = 0; i < asmClassNodes.size(); i++) {
                org.objectweb.asm.tree.ClassNode asmNode = asmClassNodes.get(i);
                try {
                    for (org.objectweb.asm.tree.MethodNode method : asmNode.methods) {
                        method.localVariables = null;
                        method.parameters = null;
                    }

                    byte[] outBytes;
                    try {
                        ClassWriter cw = new FuckyClassWriter(classWriterFlags);
                        asmNode.accept(cw);
                        outBytes = cw.toByteArray();
                    } catch (Throwable t) {
                        if (debug) {
                            System.err.println("Primary write failed for class: " + asmNode.name + ", retrying with COMPUTE_MAXS only");
                            t.printStackTrace();
                        }
                        ClassWriter cw = new FuckyClassWriter(ClassWriter.COMPUTE_MAXS);
                        asmNode.accept(cw);
                        outBytes = cw.toByteArray();
                    }

                    jos.putNextEntry(new JarEntry(asmNode.name + ".class"));
                    jos.write(outBytes);
                    jos.closeEntry();
                } catch (Throwable t) {
                    System.err.println("Failed to write class: " + asmNode.name + ", skipping. Error: " + t.getMessage());
                    if (debug) {
                        t.printStackTrace();
                    }
                }
            }

            for (Map.Entry<String, byte[]> e : resources.entrySet()) {
                try {
                    jos.putNextEntry(new ZipEntry(e.getKey()));
                    jos.write(e.getValue());
                    jos.closeEntry();
                } catch (Throwable t) {
                    System.err.println("Failed to write resource: " + e.getKey());
                    if (debug) {
                        t.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot write output jar: " + out.toAbsolutePath(), e);
        }
    }

    private static Map<String, byte[]> loadFilesFromZip(Path zipPath) {
        Map<String, byte[]> out = new LinkedHashMap<>();

        try (InputStream is = Files.newInputStream(zipPath);
             ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                String name = entry.getName();
                byte[] data = zis.readAllBytes();
                out.put(name, data);
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read jar: " + zipPath.toAbsolutePath(), e);
        }

        return out;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Path in = Path.of("input.jar");
        private Path out = Path.of("output.jar");
        private int classReaderFlags = ClassReader.EXPAND_FRAMES;
        private int classWriterFlags = ClassWriter.COMPUTE_FRAMES;
        private boolean debug;
        private List<Transformer> transformers = new ArrayList<>();

        private Builder() {
        }

        public Builder input(Path path) {
            this.in = path;
            return this;
        }

        public Builder output(Path path) {
            this.out = path;
            return this;
        }

        public Builder classReaderFlags(int flags) {
            this.classReaderFlags = flags;
            return this;
        }

        public Builder classWriterFlags(int flags) {
            this.classWriterFlags = flags;
            return this;
        }

        public Builder addTransformer(Transformer transformer) {
            this.transformers.add(transformer);
            return this;
        }

        public Builder debug() {
            this.debug = true;
            return this;
        }

        public ObfuscatorCore build() {
            return new ObfuscatorCore(this);
        }
    }
}
