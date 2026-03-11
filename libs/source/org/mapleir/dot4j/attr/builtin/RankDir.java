/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr.builtin;

import org.mapleir.dot4j.attr.Attr;

public class RankDir
extends Attr<String> {
    public static final RankDir TOP_TO_BOTTOM = new RankDir("TB");
    public static final RankDir BOTTOM_TO_TOP = new RankDir("BT");
    public static final RankDir LEFT_TO_RIGHT = new RankDir("LR");
    public static final RankDir RIGHT_TO_LEFT = new RankDir("RL");

    protected RankDir(String value) {
        super("rankdir", value);
    }
}

