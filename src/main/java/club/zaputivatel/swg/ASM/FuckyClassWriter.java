package club.zaputivatel.swg.ASM;

import org.objectweb.asm.ClassWriter;

 
 public class FuckyClassWriter extends ClassWriter {
     public FuckyClassWriter(int flags) {
         super(flags);
     }
 
     @Override
     protected String getCommonSuperClass(String type1, String type2) {
        System.out.println(type1+":"+type2);
        if (type1 == null || type2 == null) {
            return "java/lang/Object";
        }
        if (type1.equals(type2)) {
            return type1;
        }
        if (type1.startsWith("[") || type2.startsWith("[")) {
            return "java/lang/Object";
        }
        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (Throwable ex) {
            return "java/lang/Object";
        }
     }
 }
