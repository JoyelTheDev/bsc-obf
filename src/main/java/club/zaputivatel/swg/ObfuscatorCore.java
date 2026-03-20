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
    private final boolean verbose;
    private final String logFile;

    private final List<org.objectweb.asm.tree.ClassNode> asmClassNodes = new ArrayList<>();
    private final Map<String, byte[]> resources = new LinkedHashMap<>();
    private final List<Transformer> transformers;
    
    private PrintStream logStream = System.out;

    private ObfuscatorCore(Builder builder) {
        this.in = builder.in;
        this.out = builder.out;
        this.classReaderFlags = builder.classReaderFlags;
        this.classWriterFlags = builder.classWriterFlags;
        this.debug = builder.debug;
        this.verbose = builder.verbose;
        this.logFile = builder.logFile;
        this.transformers = builder.transformers;

        if (!Files.exists(in)) {
            throw new IllegalArgumentException("Input file not found: " + in.toAbsolutePath());
        }
        
        setupLogging();
    }

    private void setupLogging() {
        if (logFile != null && !logFile.isEmpty()) {
            try {
                File logFileObj = new File(logFile);
                logFileObj.getParentFile().mkdirs();
                logStream = new PrintStream(new FileOutputStream(logFileObj));
            } catch (IOException e) {
                System.err.println("Failed to create log file: " + logFile);
                logStream = System.out;
            }
        }
        
        if (debug) {
            System.setOut(logStream);
        }
    }

    public void start() {
        log("Starting obfuscation process...");
        log("Input: " + in.toAbsolutePath());
        log("Output: " + out.toAbsolutePath());
        log("Transformers enabled: " + transformers.size());
        
        long startTime = System.currentTimeMillis();
        
        load();
        process();
        save();
        
        long endTime = System.currentTimeMillis();
        log("Obfuscation completed in " + (endTime - startTime) + "ms");
        
        if (logStream != System.out) {
            logStream.close();
        }
    }
    
    private void log(String message) {
        if (debug) {
            logStream.println("[Obfuscator] " + message);
        }
    }
    
    private void logVerbose(String message) {
        if (debug && verbose) {
            logStream.println("[VERBOSE] " + message);
        }
    }

    private void load() {
        log("Loading jar: " + in.toAbsolutePath());

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

        log("Loaded classes: " + classCount);
        log("Loaded resources: " + resourceCount);
    }

    private void process() {
        int processedClasses = 0;
        int failedClasses = 0;
        
        for (org.objectweb.asm.tree.ClassNode asmNode : asmClassNodes) {
            logVerbose("Processing ASM class: " + asmNode.name);
            
            try {
                org.mapleir.asm.ClassNode mapleClass = ASM2MAPLER.convertClass(asmNode);

                @SuppressWarnings("unchecked")
                List<org.objectweb.asm.tree.MethodNode> methods = new ArrayList<>(asmNode.methods);
                boolean classFailed = false;
                
                for (org.objectweb.asm.tree.MethodNode m : methods) {
                    if ((m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                        logVerbose("Skipping abstract/native method: " + m.name);
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
                        logVerbose("Failed to build CFG: " + asmNode.name + "." + m.name + m.desc);
                        if (verbose) t.printStackTrace(logStream);
                        continue;
                    }

                    boolean transformerFailed = false;
                    for (Transformer t : transformers) {
                        try {
                            t.transform(cfg, mapleMethod);
                        } catch (Throwable ex) {
                            logVerbose("Transformer failed: " + t.getClass().getSimpleName() + " on " + asmNode.name + "." + m.name + m.desc);
                            if (verbose) ex.printStackTrace(logStream);
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
                        logVerbose("BoissinotDestructor failed: " + asmNode.name + "." + m.name + m.desc);
                        if (verbose) t.printStackTrace(logStream);
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
                            logVerbose("Failed to dump CFG: " + asmNode.name + "." + m.name + m.desc);
                            if (verbose) t.printStackTrace(logStream);
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
                        logVerbose("CFG verification failed, restoring method: " + asmNode.name + "." + m.name + m.desc);
                        if (verbose) t.printStackTrace(logStream);
                        m.instructions.clear();
                        m.tryCatchBlocks.clear();
                        m.localVariables = null;
                        originalMethod.accept(m);
                        continue;
                    }
                }
                
                if (!classFailed) {
                    processedClasses++;
                } else {
                    failedClasses++;
                }
                
            } catch (Exception e) {
                logVerbose("Failed to process class: " + asmNode.name);
                if (verbose) e.printStackTrace(logStream);
                failedClasses++;
            }
        }
        
        log("Processed classes: " + processedClasses);
        log("Failed classes: " + failedClasses);
    }

    private void save() {
        log("Saving jar: " + out.toAbsolutePath());

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(out))) {
            int savedClasses = 0;
            int failedWrites = 0;
            
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
                        logVerbose("Primary write failed for class: " + asmNode.name + ", retrying with COMPUTE_MAXS only");
                        if (verbose) t.printStackTrace(logStream);
                        ClassWriter cw = new FuckyClassWriter(ClassWriter.COMPUTE_MAXS);
                        asmNode.accept(cw);
                        outBytes = cw.toByteArray();
                    }

                    jos.putNextEntry(new JarEntry(asmNode.name + ".class"));
                    jos.write(outBytes);
                    jos.closeEntry();
                    savedClasses++;
                } catch (Throwable t) {
                    logVerbose("Failed to write class: " + asmNode.name + ", skipping. Error: " + t.getMessage());
                    if (verbose) t.printStackTrace(logStream);
                    failedWrites++;
                }
            }

            for (Map.Entry<String, byte[]> e : resources.entrySet()) {
                try {
                    jos.putNextEntry(new ZipEntry(e.getKey()));
                    jos.write(e.getValue());
                    jos.closeEntry();
                } catch (Throwable t) {
                    logVerbose("Failed to write resource: " + e.getKey());
                    if (verbose) t.printStackTrace(logStream);
                }
            }
            
            log("Saved classes: " + savedClasses);
            if (failedWrites > 0) {
                log("Failed class writes: " + failedWrites);
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
                .classReaderFlags(config.getClassLoading().getClassReaderFlags())
                .classWriterFlags(config.getClassLoading().getClassWriterFlags());
        
        if (config.getDebug().isEnabled()) {
            builder.debug();
            if (config.getDebug().isVerbose()) {
                builder.verbose();
            }
            if (config.getDebug().getLogFile() != null) {
                builder.logFile(config.getDebug().getLogFile());
            }
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
        private boolean verbose;
        private String logFile;
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
        
        public Builder verbose() {
            this.verbose = true;
            return this;
        }
        
        public Builder logFile(String logFile) {
            this.logFile = logFile;
            return this;
        }

        public ObfuscatorCore build() {
            return new ObfuscatorCore(this);
        }
    }
}
