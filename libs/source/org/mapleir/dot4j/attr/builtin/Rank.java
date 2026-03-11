/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr.builtin;

import org.mapleir.dot4j.attr.Attr;

public class Rank
extends Attr<String> {
    public static final Rank SAME = new Rank("same");
    public static final Rank MIN = new Rank("min");
    public static final Rank MAX = new Rank("max");
    public static final Rank SOURCE = new Rank("source");
    public static final Rank SINK = new Rank("sink");

    protected Rank(String value) {
        super("rank", value);
    }
}

