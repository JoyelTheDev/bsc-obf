import club.zaputivatel.swg.ObfuscatorCore;
import club.zaputivatel.swg.config.ConfigLoader;
import club.zaputivatel.swg.transformer.TransformerRegistry;

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
                    
                case "--list":
                case "-l":
                    listTransformers();
                    break;
                    
                case "--help":
                case "-h":
                    printUsage();
                    break;
                    
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void runWithConfig(String configPath) throws Exception {
        System.out.println("Loading configuration from: " + configPath);
        System.out.println();
        ObfuscatorCore obfuscator = ObfuscatorCore.fromConfig(configPath).build();
        obfuscator.start();
    }
    
    private static void listTransformers() {
        System.out.println("Available Transformers:");
        System.out.println("======================");
        for (String name : TransformerRegistry.getAvailableTransformers()) {
            System.out.println("  • " + name);
        }
    }
    
    private static void printUsage() {
        System.out.println("Java Obfuscator - Simple Configuration System");
        System.out.println();
        System.out.println("Usage: java -jar obfuscator.jar <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  --config, -c <file>     Run obfuscation with config file");
        System.out.println("  --create-config, --gen-config [file]  Create default config file");
        System.out.println("  --list, -l              List all available transformers");
        System.out.println("  --help, -h              Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar obfuscator.jar --config my-config.json");
        System.out.println("  java -jar obfuscator.jar --create-config");
        System.out.println("  java -jar obfuscator.jar --list");
        System.out.println();
    }
}
