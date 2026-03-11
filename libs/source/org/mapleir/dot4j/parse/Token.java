/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.parse;

final class Token {
    public static final int EOF = 0;
    public static final int SEMICOLON = 1;
    public static final int COMMA = 2;
    public static final int BRACE_OPEN = 3;
    public static final int BRACE_CLOSE = 4;
    public static final int EQUAL = 5;
    public static final int BRACKET_OPEN = 6;
    public static final int BRACKET_CLOSE = 7;
    public static final int COLON = 8;
    public static final int STRICT = 9;
    public static final int GRAPH = 10;
    public static final int DIGRAPH = 11;
    public static final int NODE = 12;
    public static final int EDGE = 13;
    public static final int SUBGRAPH = 14;
    public static final int ID = 16;
    public static final int MINUS_MINUS = 18;
    public static final int ARROW = 19;
    public static final int SUB_SIMPLE = 1;
    public static final int SUB_NUMERAL = 2;
    public static final int SUB_QUOTED = 3;
    public static final int SUB_HTML = 4;
    public final int type;
    public final int subtype;
    public final String value;

    public Token(int type, String value) {
        this(type, -1, value);
    }

    public Token(int type, char value) {
        this(type, -1, Character.toString(value));
    }

    public Token(int type, int subtype, String value) {
        this.type = type;
        this.subtype = subtype;
        this.value = value;
    }

    public static String desc(int type) {
        switch (type) {
            case 16: {
                return "identifier";
            }
            case 5: {
                return "=";
            }
            case 0: {
                return "end of file";
            }
            case 6: {
                return "[";
            }
            case 7: {
                return "]";
            }
            case 3: {
                return "{";
            }
            case 4: {
                return "}";
            }
        }
        return "";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Token token = (Token)o;
        if (this.type != token.type) {
            return false;
        }
        if (this.subtype != token.subtype) {
            return false;
        }
        return this.value.equals(token.value);
    }

    public int hashCode() {
        int result = this.type;
        result = 31 * result + this.subtype;
        result = 31 * result + this.value.hashCode();
        return result;
    }

    public String toString() {
        return this.type + (this.subtype >= 0 ? "(" + this.subtype + ")" : "") + "`" + this.value + "`";
    }
}

