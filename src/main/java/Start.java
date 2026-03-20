import club.zaputivatel.swg.ObfuscatorCore;
import club.zaputivatel.swg.config.ConfigLoader;

import java.nio.file.Paths;

public class Start {
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                printUsage();
                return;
            }
            
            String command = args[0];
            
            switch (command.toLowerCase()) {
                case "--config":
                case "-c":
                    if (args.length < 2) {
                        System.err.println("Error: Config file path required");
                        printUsage();
                        return;
                    }
                    runWithConfig(args[1]);
                    break;
                    
                case "--create-config":
                case "--gen-config":
                    String configPath = args.length > 1 ? args[1] : "obfuscator-config.json";
                    ConfigLoader.createDefaultConfigFile(configPath);
                    break;
                    
                case "--help":
                case "-h":
                    printUsage();
                    break;
                    
                default:
                    runDefault();
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void runWithConfig(String configPath) throws Exception {
        System.out.println("Loading config from: " + configPath);
        ObfuscatorCore obfuscator = ObfuscatorCore.fromConfig(configPath).build();
        obfuscator.start();
    }
    
    private static void runDefault() {
        System.out.println("Running with default configuration...");
        ObfuscatorCore obfuscator = ObfuscatorCore.builder()
                .input(Paths.get("test/test.jar"))
                .output(Paths.get("test/output.jar"))
                .debug()
                .addTransformer(new club.zaputivatel.swg.transformer.impl.OpaquePredictTransformer())
                .addTransformer(new club.zaputivatel.swg.transformer.impl.ControlFlowFlatteningMutator())
                .addTransformer(new club.zaputivatel.swg.transformer.impl.TrapEdgeFlowTransformer())
                .addTransformer(new club.zaputivatel.swg.transformer.impl.MutateInstrTransformer())
                .addTransformer(new club.zaputivatel.swg.transformer.impl.SwitchMutateTransformer())
                .addTransformer(new club.zaputivatel.swg.transformer.impl.BlockBreakerTransformer())
                .addTransformer(new club.zaputivatel.swg.transformer.impl.NumberObfuscationTransformer())
                .addTransformer(new club.zaputivatel.swg.transformer.impl.BlockDuplicateTransformer())
                .addTransformer(new club.zaputivatel.swg.transformer.impl.StringEncryptTransformer())
                .build();
        obfuscator.start();
    }
    
    private static void printUsage() {
        System.out.println("Java Obfuscator - Usage:");
        System.out.println();
        System.out.println("  java -jar obfuscator.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --config, -c <file>     Run with configuration file");
        System.out.println("  --create-config, --gen-config [file]  Create default config file");
        System.out.println("  --help, -h              Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar obfuscator.jar --config obfuscator-config.json");
        System.out.println("  java -jar obfuscator.jar --create-config my-config.json");
        System.out.println("  java -jar obfuscator.jar  (runs with default hardcoded configuration)");
        System.out.println();
    }
}
