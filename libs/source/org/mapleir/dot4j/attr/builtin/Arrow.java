/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr.builtin;

import org.mapleir.dot4j.attr.Attr;
import org.mapleir.dot4j.attr.Attrs;

public class Arrow
extends Attr<String> {
    public static final Arrow BOX = new Arrow("box");
    public static final Arrow CROW = new Arrow("crow");
    public static final Arrow CURVE = new Arrow("curve");
    public static final Arrow DIAMOND = new Arrow("diamond");
    public static final Arrow DOT = new Arrow("dot");
    public static final Arrow ICURVE = new Arrow("icurve");
    public static final Arrow INV = new Arrow("inv");
    public static final Arrow NONE = new Arrow("none");
    public static final Arrow NORMAL = new Arrow("normal");
    public static final Arrow TEE = new Arrow("tee");
    public static final Arrow VEE = new Arrow("vee");

    private Arrow(String key, String value) {
        super(key, value);
    }

    private Arrow(String value) {
        super("arrowhead", value);
    }

    public Arrow tail() {
        return (Arrow)this.key("arrowtail");
    }

    public Arrow open() {
        return (Arrow)this.value(((String)this.value).charAt(0) == 'o' ? (String)this.value : "o" + (String)this.value);
    }

    public Arrow left() {
        return this.arrowDir("l");
    }

    public Arrow right() {
        return this.arrowDir("r");
    }

    public Arrow and(Arrow arrow) {
        return (Arrow)this.value((String)arrow.value + (String)this.value);
    }

    public Attrs size(double size) {
        return this.config(size, null);
    }

    public Attrs dir(DirType type) {
        return this.config(0.0, type);
    }

    public Attrs config(double size, DirType type) {
        Attrs a = this;
        if (size > 0.0) {
            a = Attrs.attrs(a, Attrs.attr("arrowsize", size));
        }
        if (type != null) {
            a = Attrs.attrs(a, Attrs.attr("dir", type.name().toLowerCase()));
        }
        return a;
    }

    private Arrow arrowDir(String dir) {
        switch (((String)this.value).charAt(0)) {
            case 'l': 
            case 'r': {
                return (Arrow)this.value(dir + ((String)this.value).substring(1));
            }
            case 'o': {
                char s = ((String)this.value).charAt(1);
                return (Arrow)this.value("o" + dir + (s == 'r' || s == 'l' ? ((String)this.value).substring(2) : ((String)this.value).substring(1)));
            }
        }
        return (Arrow)this.value(dir + (String)this.value);
    }

    public static enum DirType {
        FORWARD,
        BACK,
        BOTH,
        NONE;

    }
}

