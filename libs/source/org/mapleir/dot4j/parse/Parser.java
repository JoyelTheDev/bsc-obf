/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.parse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.builtin.ComplexLabel;
import org.mapleir.dot4j.model.Compass;
import org.mapleir.dot4j.model.Context;
import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.dot4j.model.Edge;
import org.mapleir.dot4j.model.Factory;
import org.mapleir.dot4j.model.Node;
import org.mapleir.dot4j.model.PortNode;
import org.mapleir.dot4j.model.Source;
import org.mapleir.dot4j.model.Target;
import org.mapleir.dot4j.parse.Lexer;
import org.mapleir.dot4j.parse.ParserException;
import org.mapleir.dot4j.parse.Token;

public final class Parser {
    private final Lexer lexer;
    private Token token;

    public static DotGraph read(File file) throws IOException {
        return Parser.read(new InputStreamReader((InputStream)new FileInputStream(file), StandardCharsets.UTF_8), file.getName());
    }

    public static DotGraph read(InputStream is) throws IOException {
        return Parser.read(new InputStreamReader(is, StandardCharsets.UTF_8), "<input stream>");
    }

    public static DotGraph read(String dot) throws IOException {
        return Parser.read(new StringReader(dot), "<string>");
    }

    public static DotGraph read(Reader dot, String name) throws IOException {
        return new Parser(new Lexer(dot, name)).parse();
    }

    private Parser(Lexer lexer) throws IOException {
        this.lexer = lexer;
        this.nextToken();
    }

    private DotGraph parse() {
        return Context.use(ctx -> {
            DotGraph graph = Factory.graph();
            if (this.token.type == 9) {
                graph.setStrict(true);
                this.nextToken();
            }
            if (this.token.type == 11) {
                graph.setDirected(true);
            } else if (this.token.type != 10) {
                this.fail("'graph' or 'digraph' expected");
            }
            this.nextToken();
            if (this.token.type == 16) {
                graph.setName(this.label(this.token).toString());
                this.nextToken();
            }
            this.statementList(graph);
            this.assertToken(0);
            return graph;
        });
    }

    private ComplexLabel label(Token token) {
        return token.subtype == 4 ? ComplexLabel.html(token.value) : ComplexLabel.of(token.value);
    }

    private void statementList(DotGraph graph) throws IOException {
        this.assertToken(3);
        while (this.statement(graph)) {
            if (this.token.type != 1) continue;
            this.nextToken();
        }
        this.assertToken(4);
    }

    private boolean statement(DotGraph graph) throws IOException {
        Token base = this.token;
        switch (base.type) {
            case 16: {
                this.nextToken();
                if (this.token.type == 5) {
                    this.applyMutableAttributes(graph.getGraphAttr(), Arrays.asList(base, this.nextToken(16)));
                    this.nextToken();
                } else {
                    PortNode nodeId = this.nodeId(base);
                    if (this.token.type == 18 || this.token.type == 19) {
                        this.edgeStatement(graph, nodeId);
                    } else {
                        this.nodeStatement(graph, nodeId);
                    }
                }
                return true;
            }
            case 3: 
            case 14: {
                DotGraph sub = this.subgraph(graph.isDirected());
                if (this.token.type == 18 || this.token.type == 19) {
                    this.edgeStatement(graph, sub);
                } else {
                    graph.addSource(sub);
                }
                return true;
            }
            case 10: 
            case 12: 
            case 13: {
                this.attributeStatement(graph);
                return true;
            }
        }
        return false;
    }

    private DotGraph subgraph(boolean directed) {
        return Context.use(ctx -> {
            DotGraph sub = Factory.graph().setDirected(directed);
            if (this.token.type == 14) {
                this.nextToken();
                if (this.token.type == 16) {
                    sub.setName(this.label(this.token).toString());
                    this.nextToken();
                }
            }
            this.statementList(sub);
            return sub;
        });
    }

    private void edgeStatement(DotGraph graph, Source<? extends Source<?>> linkSource) throws IOException {
        ArrayList points = new ArrayList();
        points.add(linkSource);
        do {
            if (graph.isDirected() && this.token.type == 18) {
                this.fail("-- used in digraph. Use -> instead.");
            }
            if (!graph.isDirected() && this.token.type == 19) {
                this.fail("-> used in graph. Use -- instead.");
            }
            this.nextToken();
            if (this.token.type == 16) {
                Token id = this.token;
                this.nextToken();
                points.add(this.nodeId(id));
                continue;
            }
            if (this.token.type != 14 && this.token.type != 3) continue;
            points.add(this.subgraph(graph.isDirected()));
        } while (this.token.type == 18 || this.token.type == 19);
        List<Token> attrs = this.token.type == 6 ? this.attributeList() : Collections.emptyList();
        for (int i = 0; i < points.size() - 1; ++i) {
            Source from = (Source)points.get(i);
            Target to = (Target)points.get(i + 1);
            graph.addSource((Source)from.addEdge(this.applyAttributes(Edge.between(from, to), attrs)));
        }
    }

    private Compass compass(String name) {
        return Compass.of(name).orElseThrow(() -> new ParserException(this.lexer.pos, "Invalid compass value '" + name + "'"));
    }

    private void nodeStatement(DotGraph graph, PortNode nodeId) throws IOException {
        Node node = Factory.node(nodeId.getNode().getName());
        if (this.token.type == 6) {
            this.applyMutableAttributes(node, this.attributeList());
        }
        graph.addSource(node);
    }

    private PortNode nodeId(Token base) throws IOException {
        PortNode node = new PortNode().setNode(Factory.node(this.label(base)));
        if (this.token.type == 8) {
            String second = this.nextToken((int)16).value;
            this.nextToken();
            if (this.token.type == 8) {
                node.setRecord(second).setCompass(this.compass(this.nextToken((int)16).value));
                this.nextToken();
            } else if (Compass.of(second).isPresent()) {
                node.setCompass(this.compass(second));
            } else {
                node.setRecord(second);
            }
        }
        return node;
    }

    private void attributeStatement(DotGraph graph) throws IOException {
        Attributed<?> target = this.attributes(graph, this.token);
        this.nextToken();
        this.applyMutableAttributes(target, this.attributeList());
    }

    private void applyMutableAttributes(Attributed<?> attributed, List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i += 2) {
            String key = tokens.get((int)i).value;
            Token value = tokens.get(i + 1);
            if ("label".equals(key) || "xlabel".equals(key) || "headlabel".equals(key) || "taillabel".equals(key)) {
                attributed.with(key, (Object)this.label(value));
                continue;
            }
            attributed.with(key, value.value);
        }
    }

    private <T extends Attributed<T>> T applyAttributes(T attributed, List<Token> tokens) {
        Object res = attributed;
        for (int i = 0; i < tokens.size(); i += 2) {
            res = (Attributed)res.with(tokens.get((int)i).value, tokens.get((int)(i + 1)).value);
        }
        return res;
    }

    private Attributed<?> attributes(DotGraph graph, Token token) {
        switch (token.type) {
            case 10: {
                return graph.getGraphAttr();
            }
            case 12: {
                return Context.get().nodeAttrs();
            }
            case 13: {
                return Context.get().edgeAttrs();
            }
        }
        return null;
    }

    private List<Token> attributeList() throws IOException {
        ArrayList<Token> res = new ArrayList<Token>();
        do {
            this.assertToken(6);
            if (this.token.type == 16) {
                res.addAll(this.attrListElement());
            }
            this.assertToken(7);
        } while (this.token.type == 6);
        return res;
    }

    private List<Token> attrListElement() throws IOException {
        ArrayList<Token> res = new ArrayList<Token>();
        do {
            res.add(this.token);
            this.nextToken(5);
            res.add(this.nextToken(16));
            this.nextToken();
            if (this.token.type != 1 && this.token.type != 2) continue;
            this.nextToken();
        } while (this.token.type == 16);
        return res;
    }

    private Token nextToken() throws IOException {
        this.token = this.lexer.token();
        return this.token;
    }

    private Token nextToken(int type) throws IOException {
        this.nextToken();
        this.checkToken(type);
        return this.token;
    }

    private Token assertToken(int type) throws IOException {
        this.checkToken(type);
        return this.nextToken();
    }

    private void checkToken(int type) {
        if (this.token.type != type) {
            this.fail("'" + Token.desc(type) + "' expected");
        }
    }

    private void fail(String msg) {
        throw new ParserException(this.lexer.pos, msg);
    }
}

