/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.util;

import java.io.StringWriter;

public class TabbedStringWriter {
    private static final String NEWLINE = System.lineSeparator();
    private StringWriter buff = new StringWriter();
    private int tabCount = 0;
    private int lineNumber = 0;
    private int charPointer = 0;
    private String tabString = "   ";

    public TabbedStringWriter print(CharSequence str) {
        for (int i = 0; i < str.length(); ++i) {
            this.print(str.charAt(i));
        }
        return this;
    }

    public TabbedStringWriter print(char c, boolean indent) {
        this.buff.append(c);
        if (c == '\n') {
            ++this.lineNumber;
            if (indent) {
                String tabs = this.getTabs();
                this.charPointer = tabs.length();
                this.buff.append(tabs);
            } else {
                this.charPointer = 0;
            }
        } else {
            ++this.charPointer;
        }
        return this;
    }

    public TabbedStringWriter print(char c) {
        this.print(c, true);
        return this;
    }

    public TabbedStringWriter newline() {
        return this.print(NEWLINE);
    }

    public void setTabString(String tabString) {
        this.tabString = tabString;
    }

    protected String getTabString() {
        return this.tabString;
    }

    private String getTabs() {
        StringBuilder tabs = new StringBuilder();
        for (int i = 0; i < this.tabCount; ++i) {
            tabs.append(this.tabString);
        }
        return tabs.toString();
    }

    public int getTabCount() {
        return this.tabCount;
    }

    public int getLineCount() {
        return this.lineNumber;
    }

    public int getColumnOffset() {
        return this.charPointer;
    }

    public int getTextColumnOffset() {
        int tabOffset = this.tabCount * this.tabString.length();
        return this.charPointer - tabOffset;
    }

    public TabbedStringWriter tab() {
        ++this.tabCount;
        return this;
    }

    public TabbedStringWriter forceIndent() {
        this.buff.append(this.getTabs());
        return this;
    }

    public TabbedStringWriter untab() {
        if (this.tabCount <= 0) {
            throw new UnsupportedOperationException();
        }
        --this.tabCount;
        return this;
    }

    public void clear() {
        this.buff = new StringWriter();
        this.tabCount = 0;
    }

    public String toString() {
        this.buff.flush();
        return this.buff.toString();
    }
}

