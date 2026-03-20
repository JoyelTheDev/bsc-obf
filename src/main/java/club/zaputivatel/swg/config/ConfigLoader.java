package club.zaputivatel.swg.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigLoader {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    public static ObfuscatorConfig load(String configPath) throws IOException {
        return load(Paths.get(configPath));
    }
    
    public static ObfuscatorConfig load(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            System.err.println("Config file not found: " + configPath);
            return createDefaultConfig();
        }
        
        try (Reader reader = Files.newBufferedReader(configPath)) {
            return gson.fromJson(reader, ObfuscatorConfig.class);
        }
    }
    
    public static void save(ObfuscatorConfig config, String configPath) throws IOException {
        save(config, Paths.get(configPath));
    }
    
    public static void save(ObfuscatorConfig config, Path configPath) throws IOException {
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            gson.toJson(config, writer);
        }
    }
    
    public static ObfuscatorConfig createDefaultConfig() {
        ObfuscatorConfig config = new ObfuscatorConfig();
        
        // Add default transformers
        addTransformer(config, "OpaquePredictTransformer", true, 10);
        addTransformer(config, "ControlFlowFlatteningMutator", true, 20);
        addTransformer(config, "TrapEdgeFlowTransformer", true, 30);
        addTransformer(config, "MutateInstrTransformer", true, 40);
        addTransformer(config, "SwitchMutateTransformer", true, 50);
        addTransformer(config, "BlockBreakerTransformer", true, 60);
        addTransformer(config, "NumberObfuscationTransformer", true, 70);
        addTransformer(config, "BlockDuplicateTransformer", true, 80);
        addTransformer(config, "StringEncryptTransformer", true, 90);
        addTransformer(config, "CrasherTransformer", false, 100);
        addTransformer(config, "WatermarkTransformer", false, 110);
        addTransformer(config, "ShuffleTransformer", false, 120);
        addTransformer(config, "StructAccessIndyTransformer", false, 130);
        
        return config;
    }
    
    private static void addTransformer(ObfuscatorConfig config, String name, boolean enabled, int priority) {
        ObfuscatorConfig.TransformerConfig t = new ObfuscatorConfig.TransformerConfig();
        t.setName(name);
        t.setEnabled(enabled);
        t.setPriority(priority);
        config.getTransformers().add(t);
    }
    
    public static void createDefaultConfigFile(String path) throws IOException {
        ObfuscatorConfig defaultConfig = createDefaultConfig();
        save(defaultConfig, path);
        System.out.println("Created default config file at: " + path);
    }
}
