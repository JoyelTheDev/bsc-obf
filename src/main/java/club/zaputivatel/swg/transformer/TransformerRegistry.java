package club.zaputivatel.swg.transformer;

import club.zaputivatel.swg.config.ObfuscatorConfig;
import club.zaputivatel.swg.transformer.impl.*;

import java.util.*;
import java.util.function.Supplier;

public class TransformerRegistry {
    private static final Map<String, Supplier<Transformer>> REGISTRY = new LinkedHashMap<>();
    
    static {
        // Register all transformers
        register("OpaquePredictTransformer", OpaquePredictTransformer::new);
        register("ControlFlowFlatteningMutator", ControlFlowFlatteningMutator::new);
        register("TrapEdgeFlowTransformer", TrapEdgeFlowTransformer::new);
        register("MutateInstrTransformer", MutateInstrTransformer::new);
        register("SwitchMutateTransformer", SwitchMutateTransformer::new);
        register("BlockBreakerTransformer", BlockBreakerTransformer::new);
        register("NumberObfuscationTransformer", NumberObfuscationTransformer::new);
        register("BlockDuplicateTransformer", BlockDuplicateTransformer::new);
        register("StringEncryptTransformer", StringEncryptTransformer::new);
        register("CrasherTransformer", CrasherTransformer::new);
        register("WatermarkTransformer", WatermarkTransformer::new);
        register("ShuffleTransformer", ShuffleTransformer::new);
        register("StructAccessIndyTransformer", StructAccessIndyTransformer::new);
    }
    
    private static void register(String name, Supplier<Transformer> supplier) {
        REGISTRY.put(name, supplier);
    }
    
    public static Transformer create(String name) {
        Supplier<Transformer> supplier = REGISTRY.get(name);
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown transformer: " + name);
        }
        return supplier.get();
    }
    
    public static List<Transformer> createFromConfig(ObfuscatorConfig config) {
        List<Transformer> transformers = new ArrayList<>();
        
        List<ObfuscatorConfig.TransformerConfig> configs = new ArrayList<>(config.getTransformers());
        configs.sort(Comparator.comparingInt(ObfuscatorConfig.TransformerConfig::getPriority));
        
        for (ObfuscatorConfig.TransformerConfig tConfig : configs) {
            if (tConfig.isEnabled()) {
                try {
                    Transformer transformer = create(tConfig.getName());
                    transformers.add(transformer);
                    System.out.println("Enabled transformer: " + tConfig.getName());
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: " + e.getMessage());
                }
            }
        }
        
        return transformers;
    }
    
    public static Set<String> getAvailableTransformers() {
        return REGISTRY.keySet();
    }
}
