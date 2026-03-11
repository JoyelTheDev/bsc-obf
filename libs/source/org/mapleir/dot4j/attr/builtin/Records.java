/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr.builtin;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.mapleir.dot4j.attr.Attrs;

public class Records {
    private static final String SHAPE = "shape";
    private static final String RECORD = "record";
    private static final String LABEL = "label";

    private Records() {
    }

    public static Attrs label(String label) {
        return Attrs.attrs(Attrs.attr(SHAPE, RECORD), Attrs.attr(LABEL, label));
    }

    public static Attrs mLabel(String label) {
        return Attrs.attrs(Attrs.attr(SHAPE, "Mrecord"), Attrs.attr(LABEL, label));
    }

    public static Attrs of(String ... recs) {
        return Attrs.attrs(Attrs.attr(SHAPE, RECORD), Attrs.attr(LABEL, Arrays.stream(recs).collect(Collectors.joining("|"))));
    }

    public static Attrs mOf(String ... recs) {
        return Attrs.attrs(Attrs.attr(SHAPE, "Mrecord"), Attrs.attr(LABEL, Arrays.stream(recs).collect(Collectors.joining("|"))));
    }

    public static String rec(String tag, String label) {
        return "<" + tag + ">" + Records.rec(label);
    }

    public static String rec(String label) {
        return label.replace("{", "\\{").replace("}", "\\}").replace("<", "\\<").replace(">", "\\>").replace("|", "\\|").replace(" ", "\\ ");
    }

    public static String turn(String ... records) {
        return "{" + Arrays.stream(records).collect(Collectors.joining("|")) + "}";
    }
}

