/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr.builtin;

import org.mapleir.dot4j.attr.Attrs;

public class Font {
    private Font() {
    }

    public static Attrs config(String name, int size) {
        return Attrs.attrs(Font.name(name), Font.size(size));
    }

    public static Attrs config(String name, double size, double dpi) {
        return Attrs.attrs(Font.name(name), Font.size(size), Font.dpi(dpi));
    }

    public static Attrs name(String name) {
        return Attrs.attr("fontname", name);
    }

    public static Attrs size(double size) {
        return Attrs.attr("fontsize", size);
    }

    public static Attrs dpi(double dpi) {
        return Attrs.attr("dpi", dpi);
    }
}

