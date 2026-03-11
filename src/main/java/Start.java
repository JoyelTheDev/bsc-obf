import club.zaputivatel.swg.ObfuscatorCore;
import club.zaputivatel.swg.transformer.impl.*;

import java.nio.file.Paths;

public class Start {
    public static void main(String[] args) {
        ObfuscatorCore builder = ObfuscatorCore.builder()
                .input(Paths.get("test/test.jar"))
                .output(Paths.get("test/outpu2t.jar"))
                .debug()
                    .addTransformer(new StructAccessIndyTransformer())
                .addTransformer(new NumberObfuscationTransformer())
                .addTransformer(new BlockDuplicateTransformer())
                .addTransformer(new StringEncryptTransformer())
                //   .addTransformer(new CrasherTransformer())
             //   .addTransformer(new WatermarkTransformer())
                .addTransformer(new ControlFlowFlatteningMutator())



                .build();
        builder.start();
    }
}
