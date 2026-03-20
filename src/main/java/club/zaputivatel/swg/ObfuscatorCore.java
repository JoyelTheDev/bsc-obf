package club.zaputivatel.swg.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ObfuscatorConfig {
    private InputConfig input = new InputConfig();
    private OutputConfig output = new OutputConfig();
    private Map<String, Boolean> transformers = new LinkedHashMap<>();
    private DebugConfig debug = new DebugConfig();

    public static class InputConfig {
        private String path = "test/test.jar";
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    public static class OutputConfig {
        private String path = "test/output.jar";
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    public static class DebugConfig {
        private boolean enabled = false;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public InputConfig getInput() { return input; }
    public void setInput(InputConfig input) { this.input = input; }
    public OutputConfig getOutput() { return output; }
    public void setOutput(OutputConfig output) { this.output = output; }
    public Map<String, Boolean> getTransformers() { return transformers; }
    public void setTransformers(Map<String, Boolean> transformers) { this.transformers = transformers; }
    public DebugConfig getDebug() { return debug; }
    public void setDebug(DebugConfig debug) { this.debug = debug; }
    
    public Path getInputPath() { return Paths.get(input.getPath()); }
    public Path getOutputPath() { return Paths.get(output.getPath()); }
}
