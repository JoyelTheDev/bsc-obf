package club.zaputivatel.swg;

import club.zaputivatel.swg.ASM.FuckyClassWriter;
import club.zaputivatel.swg.ASM.helper.ASMHelper;
import club.zaputivatel.swg.config.ConfigLoader;
import club.zaputivatel.swg.config.ObfuscatorConfig;
import club.zaputivatel.swg.mapleIR.ASM2MAPLER;
import club.zaputivatel.swg.transformer.Transformer;
import club.zaputivatel.swg.transformer.TransformerRegistry;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.builder.ControlFlowGraphBuilder;
import org.mapleir.ir.codegen.ControlFlowGraphDumper;
import org.mapleir.ir.algorithms.BoissinotDestructor;
import org.mapleir.ir.algorithms.LocalsReallocator;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
        if (debug) {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║           Java Obfuscator - Starting Process             ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("Input:  " + in.toAbsolutePath());
            System.out.println("Output: " + out.toAbsolutePath());
            System.out.println("Transformers: " + transformers.size());
            System.out.println();
        }
        
        long startTime = System.currentTimeMillis();
        
        load();
        process();
        save();
        
        long endTime = System.currentTimeMillis();
        
        if (debug) {
            System.out.println();
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║           Obfuscation Completed Successfully!            ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println("Time elapsed: " + (endTime - startTime) + "ms");
            System.out.println("Output saved to: " + out.toAbsolutePath());
        }
    }

    private void load() {
        if (debug) {
            System.out.println("[1/3] Loading JAR file...");
        }

        Map<String, byte[]> files = loadFilesFromZip(in);
        int classCount = 0;
        int resourceCount = 0;
        
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            String name = e.getKey();
            byte[] data = e.getValue();

            if (ASMHelper.isClass(name, data)) {
                org.objectweb.asm.tree.ClassNode asmNode = new org.objectweb.asm.tree.ClassNode();
                org.objectweb.asm.ClassReader reader = new org.objectweb.asm.ClassReader(data);
                reader.accept(asmNode, classReaderFlags);
                asmClassNodes.add(asmNode);
                classCount++;
            } else {
                resources.put(name, data);
                resourceCount++;
            }
        }

        if (debug) {
            System.out.println("  ✓ Loaded " + classCount + " classes");
            System.out.println("  ✓ Loaded " + resourceCount + " resources");
            System.out.println();
        }
    }

    private void process() {
        if (debug) {
            System.out.println("[2/3] Processing classes...");
        }
        
        int processedClasses = 0;
        int failedClasses = 0;
        
        for (org.objectweb.asm.tree.ClassNode asmNode : asmClassNodes) {
            if (debug) {
                System.out.print("  Processing: " + asmNode.name + " ... ");
            }
            
            try {
                org.mapleir.asm.ClassNode mapleClass = ASM2MAPLER.convertClass(asmNode);

                @SuppressWarnings("unchecked")
                List<org.objectweb.asm.tree.MethodNode> methods = new ArrayList<>(asmNode.methods);
                boolean classFailed = false;
                
                for (org.objectweb.asm.tree.MethodNode m : methods) {
                    if ((m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                        continue;
                    }

                    org.objectweb.asm.tree.MethodNode originalMethod = new org.objectweb.asm.tree.MethodNode(
                        m.access, m.name, m.desc, m.signature, m.exceptions.toArray(new String[0])
                    );
                    m.accept(originalMethod);

                    org.mapleir.asm.MethodNode mapleMethod = new org.mapleir.asm.MethodNode(m, mapleClass);
                    ControlFlowGraph cfg;
                    try {
                        cfg = ControlFlowGraphBuilder.build(mapleMethod);
                    } catch (Throwable t) {
                        if (debug && classFailed == false) {
                            System.out.print("(CFG failed) ");
                        }
                        continue;
                    }

                    boolean transformerFailed = false;
                    for (Transformer t : transformers) {
                        try {
                            t.transform(cfg, mapleMethod);
                        } catch (Throwable ex) {
                            if (debug) {
                                System.out.print("(" + t.getClass().getSimpleName() + " failed) ");
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
                            System.out.print("(SSA failed) ");
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
                                System.out.print("(dump failed) ");
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
                            System.out.print("(verify failed) ");
                        }
                        m.instructions.clear();
                        m.tryCatchBlocks.clear();
                        m.localVariables = null;
                        originalMethod.accept(m);
                        continue;
                    }
                }
                
                if (!classFailed) {
                    processedClasses++;
                    if (debug) {
                        System.out.println("✓");
                    }
                } else {
                    failedClasses++;
                    if (debug) {
                        System.out.println("✗");
                    }
                }
                
            } catch (Exception e) {
                failedClasses++;
                if (debug) {
                    System.out.println("✗ (error)");
                }
            }
        }
        
        if (debug) {
            System.out.println();
            System.out.println("  Processed: " + processedClasses + " classes");
            if (failedClasses > 0) {
                System.out.println("  Failed: " + failedClasses + " classes");
            }
            System.out.println();
        }
    }

    private void save() {
        if (debug) {
            System.out.println("[3/3] Saving obfuscated JAR...");
        }

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(out))) {
            int savedClasses = 0;
            
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
                        ClassWriter cw = new FuckyClassWriter(ClassWriter.COMPUTE_MAXS);
                        asmNode.accept(cw);
                        outBytes = cw.toByteArray();
                    }

                    jos.putNextEntry(new JarEntry(asmNode.name + ".class"));
                    jos.write(outBytes);
                    jos.closeEntry();
                    savedClasses++;
                } catch (Throwable t) {
                    if (debug) {
                        System.err.println("  Failed to write class: " + asmNode.name);
                    }
                }
            }

            for (Map.Entry<String, byte[]> e : resources.entrySet()) {
                try {
                    jos.putNextEntry(new ZipEntry(e.getKey()));
                    jos.write(e.getValue());
                    jos.closeEntry();
                } catch (Throwable t) {
                    if (debug) {
                        System.err.println("  Failed to write resource: " + e.getKey());
                    }
                }
            }
            
            if (debug) {
                System.out.println("  ✓ Saved " + savedClasses + " classes");
                System.out.println();
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
    
    public static Builder fromConfig(String configPath) throws IOException {
        return fromConfig(ConfigLoader.load(configPath));
    }
    
    public static Builder fromConfig(Path configPath) throws IOException {
        return fromConfig(ConfigLoader.load(configPath));
    }
    
    public static Builder fromConfig(ObfuscatorConfig config) {
        Builder builder = new Builder()
                .input(config.getInputPath())
                .output(config.getOutputPath())
                .classReaderFlags(ClassReader.EXPAND_FRAMES)
                .classWriterFlags(ClassWriter.COMPUTE_FRAMES);
        
        if (config.getDebug().isEnabled()) {
            builder.debug();
        }
        
        List<Transformer> transformers = TransformerRegistry.createFromConfig(config);
        for (Transformer t : transformers) {
            builder.addTransformer(t);
        }
        
        return builder;
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
