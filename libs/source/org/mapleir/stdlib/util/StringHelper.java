/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.util;

public class StringHelper {
    public static int numeric(String label) {
        int result = 0;
        for (int i = label.length() - 1; i >= 0; --i) {
            result += (label.charAt(i) - 64) * (int)Math.pow(26.0, label.length() - (i + 1));
        }
        return result;
    }

    public static String createBlockName(int n) {
        char[] buf = new char[(int)Math.floor(Math.log(25 * (n + 1)) / Math.log(26.0))];
        for (int i = buf.length - 1; i >= 0; --i) {
            buf[i] = (char)(65 + --n % 26);
            n /= 26;
        }
        return String.valueOf(buf);
    }
}

