/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.graph.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mapleir.stdlib.collections.graph.FastDirectedGraph;
import org.mapleir.stdlib.collections.graph.FastGraphEdge;
import org.mapleir.stdlib.collections.graph.FastGraphEdgeImpl;
import org.mapleir.stdlib.collections.graph.FastGraphVertex;
import org.mapleir.stdlib.collections.graph.algorithms.DominatorTree;
import org.mapleir.stdlib.collections.graph.algorithms.ExtendedDfs;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;
import org.mapleir.stdlib.collections.map.SetCreator;

public class LT79Dom<N extends FastGraphVertex, E extends FastGraphEdge<N>> {
    private final FastDirectedGraph<N, E> graph;
    private final N root;
    private final boolean computeFrontiers;
    private final Map<N, Integer> semi;
    private final Map<Integer, N> vertex;
    private final List<N> postOrder;
    private final Map<N, N> parent;
    private final Map<N, N> idoms;
    private final NullPermeableHashMap<N, Set<N>> bucket;
    private final Map<N, N> ancestor;
    private final Map<N, N> label;
    private final NullPermeableHashMap<N, Set<N>> treeDescendants;
    private final NullPermeableHashMap<N, Set<N>> treeSuccessors;
    private final DominatorTree<N> dominatorTree;
    private final NullPermeableHashMap<N, Set<N>> frontiers;
    private final NullPermeableHashMap<N, Set<N>> iteratedFrontiers;

    public LT79Dom(FastDirectedGraph<N, E> graph, N root) {
        this(graph, root, true);
    }

    public LT79Dom(FastDirectedGraph<N, E> graph, N root, boolean computeFrontiers) {
        this.graph = graph;
        this.root = root;
        this.semi = new HashMap<N, Integer>();
        this.vertex = new HashMap<Integer, N>();
        this.postOrder = new ArrayList<N>();
        this.parent = new HashMap<N, N>();
        this.idoms = new HashMap<N, N>();
        this.bucket = new NullPermeableHashMap(new SetCreator());
        this.ancestor = new HashMap<N, N>();
        this.label = new HashMap<N, N>();
        this.treeDescendants = new NullPermeableHashMap(new SetCreator());
        this.treeSuccessors = new NullPermeableHashMap(new SetCreator());
        this.frontiers = new NullPermeableHashMap(new SetCreator());
        this.iteratedFrontiers = new NullPermeableHashMap(new SetCreator());
        this.step1();
        this.step2and3();
        this.step4();
        this.dominatorTree = this.makeDominatorTree();
        this.computeFrontiers = computeFrontiers;
        if (computeFrontiers) {
            this.dfrontiers();
            this.iteratedFrontiers();
        }
    }

    public List<N> getPreOrder() {
        ArrayList<FastGraphVertex> preOrder = new ArrayList<FastGraphVertex>();
        for (int i = 0; i < this.vertex.size(); ++i) {
            preOrder.add((FastGraphVertex)this.vertex.get(i));
        }
        return preOrder;
    }

    public List<N> getPostOrder() {
        return new ArrayList<N>(this.postOrder);
    }

    private void step1() {
        this.dfs(this.root);
        assert (this.vertex.get(0) == this.root);
    }

    private void dfs(N v) {
        int n = this.semi.size();
        this.semi.put(v, n);
        this.vertex.put(n, v);
        this.ancestor.put(v, null);
        this.label.put(v, v);
        for (FastGraphEdge succ : this.graph.getEdges(v)) {
            Object w = succ.dst();
            if (this.semi.containsKey(w)) continue;
            this.parent.put(w, v);
            this.dfs(w);
        }
        this.postOrder.add(v);
    }

    private void step2and3() {
        for (int i = this.semi.size() - 1; i > 0; --i) {
            FastGraphVertex w = (FastGraphVertex)this.vertex.get(i);
            this.step2(w);
            this.step3(w);
        }
    }

    private void step2(N w) {
        for (FastGraphEdge pred : this.graph.getReverseEdges(w)) {
            Object v = pred.src();
            Object u = this.eval(v);
            if (this.semi.get(u) >= this.semi.get(w)) continue;
            this.semi.put(w, this.semi.get(u));
        }
        this.bucket.getNonNull((FastGraphVertex)this.vertex.get(this.semi.get(w))).add(w);
        this.link((FastGraphVertex)this.parent.get(w), w);
    }

    private void step3(N w) {
        Set<N> wbucket = this.bucket.getNonNull((FastGraphVertex)this.parent.get(w));
        for (FastGraphVertex v : wbucket) {
            FastGraphVertex u = this.eval(v);
            FastGraphVertex dom = this.semi.get(u) < this.semi.get(v) ? u : (FastGraphVertex)this.parent.get(w);
            this.idoms.put(v, dom);
        }
        wbucket.clear();
    }

    private void step4() {
        for (int i = 0; i < this.semi.size(); ++i) {
            FastGraphVertex w = (FastGraphVertex)this.vertex.get(i);
            if (this.idoms.get(w) == this.vertex.get(this.semi.get(w))) continue;
            this.idoms.put(w, (FastGraphVertex)this.idoms.get(this.idoms.get(w)));
        }
    }

    private void link(N v, N w) {
        this.ancestor.put(w, v);
    }

    private N eval(N v) {
        if (this.ancestor.get(v) != null) {
            this.compress(v);
            return (N)((FastGraphVertex)this.label.get(v));
        }
        return v;
    }

    private void compress(N v) {
        if (this.ancestor.get(this.ancestor.get(v)) != null) {
            this.compress((FastGraphVertex)this.ancestor.get(v));
            if (this.semi.get(this.label.get(this.ancestor.get(v))) < this.semi.get(this.label.get(v))) {
                this.label.put(v, (FastGraphVertex)this.label.get(this.ancestor.get(v)));
            }
            this.ancestor.put(v, (FastGraphVertex)this.ancestor.get(this.ancestor.get(v)));
        }
    }

    private void dfrontiers() {
        for (FastGraphVertex n : this.treeReverseTopoOrder()) {
            Set<N> df = this.frontiers.getNonNull(n);
            for (FastGraphEdge e : this.graph.getEdges(n)) {
                Object succ = e.dst();
                if (this.idoms.get(succ) == n) continue;
                df.add(succ);
            }
            for (FastGraphVertex f : this.treeSuccessors.getNonNull(n)) {
                for (FastGraphVertex ff : this.frontiers.getNonNull(f)) {
                    if (this.idoms.get(ff) == n) continue;
                    df.add(ff);
                }
            }
        }
    }

    private List<N> treeReverseTopoOrder() {
        ExtendedDfs<N> dfs = new ExtendedDfs<N>(this.getDominatorTree(), 8);
        dfs.run(this.root);
        return dfs.getPostOrder();
    }

    private void iteratedFrontiers() {
        for (FastGraphVertex n : this.postOrder) {
            this.iteratedFrontier(n);
        }
    }

    private void iteratedFrontier(N n) {
        HashSet<FastGraphVertex> newWorkingSet;
        HashSet<FastGraphVertex> res = new HashSet<FastGraphVertex>();
        HashSet<N> workingSet = new HashSet<N>();
        workingSet.add(n);
        do {
            newWorkingSet = new HashSet<FastGraphVertex>();
            for (FastGraphVertex n1 : workingSet) {
                for (FastGraphVertex n2 : (Set)this.frontiers.get(n1)) {
                    if (res.contains(n2)) continue;
                    newWorkingSet.add(n2);
                    res.add(n2);
                }
            }
        } while (!(workingSet = newWorkingSet).isEmpty());
        this.iteratedFrontiers.put(n, res);
    }

    private DominatorTree<N> makeDominatorTree() {
        DominatorTree tree = new DominatorTree();
        for (FastGraphVertex v : this.postOrder) {
            FastGraphVertex idom = (FastGraphVertex)this.idoms.get(v);
            if (idom != null) {
                Set<N> decs = this.treeDescendants.getNonNull(idom);
                decs.add(v);
                decs.addAll((Collection)this.treeDescendants.getNonNull(v));
                Set<N> succs = this.treeSuccessors.getNonNull(idom);
                succs.add(v);
                tree.addEdge(new FastGraphEdgeImpl<FastGraphVertex>(idom, v));
            }
            this.treeDescendants.getNonNull(v).add(v);
        }
        return tree;
    }

    public DominatorTree<N> getDominatorTree() {
        return this.dominatorTree;
    }

    public Set<N> getDominates(N v) {
        return new HashSet(this.treeDescendants.getNonNull(v));
    }

    public N getImmediateDominator(N v) {
        return (N)((FastGraphVertex)this.idoms.get(v));
    }

    public Set<N> getDominanceFrontier(N v) {
        if (this.computeFrontiers) {
            return new HashSet(this.frontiers.getNonNull(v));
        }
        throw new UnsupportedOperationException();
    }

    public Set<N> getIteratedDominanceFrontier(N v) {
        if (this.computeFrontiers) {
            return new HashSet(this.iteratedFrontiers.getNonNull(v));
        }
        throw new UnsupportedOperationException();
    }
}

