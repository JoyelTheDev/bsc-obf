/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.parse;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import org.mapleir.dot4j.parse.ParserException;
import org.mapleir.dot4j.parse.Position;
import org.mapleir.dot4j.parse.Token;

class Lexer {
    private static final Map<String, Integer> KEYWORDS = new HashMap<String, Integer>();
    private static final char CH_EOF = '\uffff';
    private final PushbackReader in;
    private char ch;
    Position pos;

    public Lexer(Reader in, String name) throws IOException {
        this.in = new PushbackReader(in);
        this.pos = new Position(name);
        this.readChar();
    }

    public Token token() throws IOException {
        Token sym = this.symbol();
        if (sym != null) {
            this.readChar();
            return sym;
        }
        return this.numeralOrIdent();
    }

    private Token symbol() throws IOException {
        switch (this.ch) {
            case '\uffff': {
                return new Token(0, this.ch);
            }
            case ';': {
                return new Token(1, this.ch);
            }
            case ',': {
                return new Token(2, this.ch);
            }
            case '{': {
                return new Token(3, this.ch);
            }
            case '}': {
                return new Token(4, this.ch);
            }
            case '=': {
                return new Token(5, this.ch);
            }
            case '[': {
                return new Token(6, this.ch);
            }
            case ']': {
                return new Token(7, this.ch);
            }
            case ':': {
                return new Token(8, this.ch);
            }
            case '-': {
                char next = this.readRawChar();
                if (next == '-') {
                    return new Token(18, "--");
                }
                if (next == '>') {
                    return new Token(19, "->");
                }
                this.unread('-', next);
                return null;
            }
        }
        return null;
    }

    private Token numeralOrIdent() throws IOException {
        if (this.ch == '-' || this.ch == '.' || this.ch >= '0' && this.ch <= '9') {
            return this.numeral();
        }
        return this.ident();
    }

    private Token numeral() throws IOException {
        StringBuilder s = new StringBuilder();
        do {
            s.append(this.ch);
            this.readRawChar();
        } while (this.ch == '.' || this.ch >= '0' && this.ch <= '9');
        this.sync();
        return new Token(16, 2, s.toString());
    }

    private Token ident() throws IOException {
        if (this.ch == '\"') {
            return this.quotedIdent();
        }
        if (this.ch == '<') {
            return this.htmlIdent();
        }
        if (this.isIdentStart()) {
            return this.simpleIdent();
        }
        throw new ParserException(this.pos, "Found unexpected character '" + this.ch + "'");
    }

    private boolean isIdentStart() {
        return this.ch >= 'a' && this.ch <= 'z' || this.ch >= 'A' && this.ch <= 'Z' || this.ch >= '\u0080' && this.ch <= '\u00ff' || this.ch == '_';
    }

    private Token quotedIdent() throws IOException {
        StringBuilder s = new StringBuilder();
        this.readRawChar();
        while (this.ch != '\"' && this.ch != '\uffff') {
            s.append(this.ch);
            this.readRawChar();
            if (this.ch == '\"' && s.charAt(s.length() - 1) == '\\') {
                s.replace(s.length() - 1, s.length(), "\"");
                this.readRawChar();
            }
            if (this.ch != '\n' || s.charAt(s.length() - 1) != '\\') continue;
            s.delete(s.length() - 1, s.length());
            this.readRawChar();
        }
        this.readChar();
        return new Token(16, 3, s.toString());
    }

    private Token htmlIdent() throws IOException {
        StringBuilder s = new StringBuilder();
        int level = 1;
        this.readRawChar();
        level = this.htmlLevel(level, this.ch);
        while ((this.ch != '>' || level > 0) && this.ch != '\uffff') {
            s.append(this.ch);
            this.readRawChar();
            level = this.htmlLevel(level, this.ch);
        }
        this.readChar();
        return new Token(16, 4, s.toString());
    }

    private int htmlLevel(int level, char ch) {
        if (ch == '<') {
            return level + 1;
        }
        if (ch == '>') {
            return level - 1;
        }
        return level;
    }

    private Token simpleIdent() throws IOException {
        StringBuilder s = new StringBuilder();
        do {
            s.append(this.ch);
            this.readRawChar();
        } while ((this.isIdentStart() || this.ch >= '0' && this.ch <= '9') && this.ch != '\uffff');
        this.sync();
        Integer key = KEYWORDS.get(s.toString().toLowerCase());
        return key == null ? new Token(16, 1, s.toString()) : new Token((int)key, s.toString());
    }

    private void sync() throws IOException {
        if (this.ch <= ' ') {
            this.readChar();
        }
    }

    private char readChar() throws IOException {
        do {
            this.readRawChar();
            if (this.ch == '/') {
                this.readComment();
                continue;
            }
            if (this.ch != '\n') continue;
            this.pos.newLine();
            char next = this.readRawChar();
            if (next == '#') {
                do {
                    this.readRawChar();
                } while (this.ch != '\n' && this.ch != '\uffff');
                continue;
            }
            this.unread('\n', next);
        } while (this.ch <= ' ' && this.ch != '\uffff');
        return this.ch;
    }

    private void readComment() throws IOException {
        char next = this.readRawChar();
        if (next == '/') {
            do {
                this.readRawChar();
            } while (this.ch != '\n' && this.ch != '\uffff');
        } else if (next == '*') {
            while (true) {
                this.readRawChar();
                if (this.ch != '*' && this.ch != '\uffff') continue;
                this.readRawChar();
                if (this.ch == '/' || this.ch == '\uffff') break;
            }
            this.readRawChar();
        } else {
            this.unread('/', next);
        }
    }

    private char readRawChar() throws IOException {
        this.pos.newChar();
        this.ch = (char)this.in.read();
        return this.ch;
    }

    private void unread(char before, char next) throws IOException {
        this.ch = before;
        this.in.unread(next);
    }

    static {
        KEYWORDS.put("strict", 9);
        KEYWORDS.put("graph", 10);
        KEYWORDS.put("digraph", 11);
        KEYWORDS.put("node", 12);
        KEYWORDS.put("edge", 13);
        KEYWORDS.put("subgraph", 14);
    }
}

