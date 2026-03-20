package club.zaputivatel.swg.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

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
        
        // All transformers with default enabled/disabled
        config.getTransformers().put("OpaquePredictTransformer", true);
        config.getTransformers().put("ControlFlowFlatteningMutator", true);
        config.getTransformers().put("TrapEdgeFlowTransformer", true);
        config.getTransformers().put("MutateInstrTransformer", true);
        config.getTransformers().put("SwitchMutateTransformer", true);
        config.getTransformers().put("BlockBreakerTransformer", true);
        config.getTransformers().put("NumberObfuscationTransformer", true);
        config.getTransformers().put("BlockDuplicateTransformer", true);
        config.getTransformers().put("StringEncryptTransformer", true);
        config.getTransformers().put("CrasherTransformer", false);
        config.getTransformers().put("WatermarkTransformer", false);
        config.getTransformers().put("ShuffleTransformer", false);
        config.getTransformers().put("StructAccessIndyTransformer", false);
        
        return config;
    }
    
    public static void createDefaultConfigFile(String path) throws IOException {
        ObfuscatorConfig defaultConfig = createDefaultConfig();
        save(defaultConfig, path);
        System.out.println("Created default config file at: " + path);
    }
}
