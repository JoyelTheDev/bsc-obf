/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.SimpleAttributed;
import org.mapleir.dot4j.attr.builtin.ComplexLabel;
import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.dot4j.model.Edge;
import org.mapleir.dot4j.model.Node;
import org.mapleir.dot4j.model.Source;
import org.mapleir.dot4j.model.Target;
import org.mapleir.dot4j.model.ThrowingFunction;

public class Context {
    private static final ThreadLocal<Stack<Context>> CONTEXT = ThreadLocal.withInitial(Stack::new);
    private DotGraph graph;
    private final Map<ComplexLabel, Node> nodes = new HashMap<ComplexLabel, Node>();
    private final Attributed<Context> nodeAttributes = new SimpleAttributed<Context>(this);
    private final Attributed<Context> edgeAttributes = new SimpleAttributed<Context>(this);
    private final Attributed<Context> graphAttributes = new SimpleAttributed<Context>(this);

    private Context() {
        this(null);
    }

    private Context(DotGraph graph) {
        this.graph = graph;
    }

    public Attributed<Context> nodeAttrs() {
        return this.nodeAttributes;
    }

    public Attributed<Context> edgeAttrs() {
        return this.edgeAttributes;
    }

    public Attributed<Context> graphAttrs() {
        return this.graphAttributes;
    }

    private Node newNode(ComplexLabel label) {
        return this.nodes.computeIfAbsent(label, l -> this.addNode(new Node().setName((ComplexLabel)l).with((Attrs)this.nodeAttributes)));
    }

    private Node addNode(Node node) {
        if (this.graph != null) {
            this.graph.addSource(node);
        }
        return node;
    }

    private DotGraph newGraph() {
        DotGraph graph = new DotGraph();
        if (this.graph != null) {
            this.graph = graph;
        }
        return graph;
    }

    static Edge createEdge(Source<?> source, Target target) {
        Edge edge = new Edge(source, target, Attrs.attrs(new Attrs[0]));
        return Context.current().map(ctx -> edge.with((Attrs)ctx.edgeAttributes)).orElse(edge);
    }

    static Node createNode(ComplexLabel label) {
        return Context.current().map(ctx -> ctx.newNode(label)).orElseGet(() -> new Node().setName(label));
    }

    static DotGraph createGraph() {
        return Context.current().map(Context::newGraph).orElseGet(DotGraph::new);
    }

    public static <T> T use(ThrowingFunction<Context, T> actions) {
        return Context.use(null, actions);
    }

    public static <T> T use(DotGraph graph, ThrowingFunction<Context, T> actions) {
        Context ctx = Context.begin(graph);
        try {
            T t = actions.apply(ctx);
            return t;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            Context.end();
        }
    }

    public static Context begin(DotGraph graph) {
        Context ctx = new Context(graph);
        CONTEXT.get().push(ctx);
        return ctx;
    }

    public static void end() {
        Stack<Context> cs = CONTEXT.get();
        if (!cs.empty()) {
            Context ctx = cs.pop();
            if (ctx.graph != null) {
                ctx.graph.getGraphAttr().with((Attrs)ctx.graphAttributes);
            }
        }
    }

    public static Optional<Context> current() {
        Stack<Context> cs = CONTEXT.get();
        return cs.empty() ? Optional.empty() : Optional.of(cs.peek());
    }

    public static Context get() {
        Stack<Context> cs = CONTEXT.get();
        if (cs.empty()) {
            throw new IllegalStateException("Not in a context");
        }
        return cs.peek();
    }
}

