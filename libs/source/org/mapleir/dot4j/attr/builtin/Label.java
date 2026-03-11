/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr.builtin;

public class Label {
    final String value;
    final boolean html;

    public Label(String value, boolean html) {
        this.value = value;
        this.html = html;
    }

    public boolean isHtml() {
        return this.html;
    }

    public String serialised() {
        if (this.html) {
            return "<" + this.value + ">";
        }
        return "\"" + this.value.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    public String toString() {
        return this.value;
    }

    public static Label of(String value) {
        return new Label(value, false);
    }

    public static Label of(Object value) {
        return value instanceof Label ? (Label)value : Label.of(value.toString());
    }
}

