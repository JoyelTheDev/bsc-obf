/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr.builtin;

import org.mapleir.dot4j.attr.Attr;

public class Style
extends Attr<String> {
    public static final Style DASHED = new Style("dashed");
    public static final Style DOTTED = new Style("dotted");
    public static final Style SOLID = new Style("solid");
    public static final Style INVIS = new Style("invis");
    public static final Style BOLD = new Style("bold");
    public static final Style FILLED = new Style("filled");
    public static final Style RADIAL = new Style("radial");
    public static final Style DIAGONALS = new Style("diagonals");
    public static final Style ROUNDED = new Style("rounded");

    public Style(String value) {
        super("style", value);
    }

    public static Style lineWidth(int width) {
        return new Style("setlinewidth(" + width + ")");
    }

    public Style and(Style style) {
        return new Style((String)this.value + "," + (String)style.value);
    }
}

