/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.parse;

import org.mapleir.dot4j.parse.Position;

public class ParserException
extends RuntimeException {
    private final Position position;

    public ParserException(Position position, String message) {
        super(message);
        this.position = position;
    }

    @Override
    public String toString() {
        return this.position.toString() + " " + this.getMessage();
    }

    public Position getPosition() {
        return this.position;
    }
}

