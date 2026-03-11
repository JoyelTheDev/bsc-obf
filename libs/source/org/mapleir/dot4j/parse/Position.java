/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.parse;

class Position {
    final String name;
    int line;
    int col;

    public Position(String name) {
        this.name = name;
        this.col = 1;
        this.line = 1;
    }

    public void newLine() {
        this.col = 1;
        ++this.line;
    }

    public void newChar() {
        ++this.col;
    }

    public String toString() {
        return this.name + ":" + this.line + ":" + this.col;
    }
}

