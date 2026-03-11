/*
 * Decompiled with CFR 0.152.
 */
package junit.runner;

public class Version {
    private Version() {
    }

    public static String id() {
        return "4.12";
    }

    public static void main(String[] args) {
        System.out.println(Version.id());
    }
}

