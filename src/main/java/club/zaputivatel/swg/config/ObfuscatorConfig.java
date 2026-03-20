package club.zaputivatel.swg.config;

import com.google.gson.annotations.SerializedName;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ObfuscatorConfig {
    private InputConfig input = new InputConfig();
    private OutputConfig output = new OutputConfig();
    private List<TransformerConfig> transformers = new ArrayList<>();
    private DebugConfig debug = new DebugConfig();
    private ClassLoadingConfig classLoading = new ClassLoadingConfig();

    public static class InputConfig {
        private String path = "test/test.jar";
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    public static class OutputConfig {
        private String path = "test/output.jar";
        private boolean overwrite = true;
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public boolean isOverwrite() { return overwrite; }
        public void setOverwrite(boolean overwrite) { this.overwrite = overwrite; }
    }

    public static class TransformerConfig {
        private String name;
        private boolean enabled = true;
        private int priority = 0;
        private java.util.Map<String, Object> parameters = new java.util.HashMap<>();
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        public java.util.Map<String, Object> getParameters() { return parameters; }
        public void setParameters(java.util.Map<String, Object> parameters) { this.parameters = parameters; }
    }

    public static class DebugConfig {
        private boolean enabled = false;
        private boolean verbose = false;
        private String logFile = null;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isVerbose() { return verbose; }
        public void setVerbose(boolean verbose) { this.verbose = verbose; }
        public String getLogFile() { return logFile; }
        public void setLogFile(String logFile) { this.logFile = logFile; }
    }

    public static class ClassLoadingConfig {
        private int classReaderFlags = org.objectweb.asm.ClassReader.EXPAND_FRAMES;
        private int classWriterFlags = org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
        private boolean skipAbstractClasses = true;
        private boolean skipNativeMethods = true;
        
        public int getClassReaderFlags() { return classReaderFlags; }
        public void setClassReaderFlags(int classReaderFlags) { this.classReaderFlags = classReaderFlags; }
        public int getClassWriterFlags() { return classWriterFlags; }
        public void setClassWriterFlags(int classWriterFlags) { this.classWriterFlags = classWriterFlags; }
        public boolean isSkipAbstractClasses() { return skipAbstractClasses; }
        public void setSkipAbstractClasses(boolean skipAbstractClasses) { this.skipAbstractClasses = skipAbstractClasses; }
        public boolean isSkipNativeMethods() { return skipNativeMethods; }
        public void setSkipNativeMethods(boolean skipNativeMethods) { this.skipNativeMethods = skipNativeMethods; }
    }

    public InputConfig getInput() { return input; }
    public void setInput(InputConfig input) { this.input = input; }
    public OutputConfig getOutput() { return output; }
    public void setOutput(OutputConfig output) { this.output = output; }
    public List<TransformerConfig> getTransformers() { return transformers; }
    public void setTransformers(List<TransformerConfig> transformers) { this.transformers = transformers; }
    public DebugConfig getDebug() { return debug; }
    public void setDebug(DebugConfig debug) { this.debug = debug; }
    public ClassLoadingConfig getClassLoading() { return classLoading; }
    public void setClassLoading(ClassLoadingConfig classLoading) { this.classLoading = classLoading; }
    
    public Path getInputPath() { return Paths.get(input.getPath()); }
    public Path getOutputPath() { return Paths.get(output.getPath()); }
}
