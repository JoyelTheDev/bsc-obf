/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.list;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

public class Worklist<N> {
    private final LinkedList<N> worklist = new LinkedList();
    private final Set<N> processed = new HashSet<N>();
    private final LinkedBlockingDeque<Worker<N>> workers = new LinkedBlockingDeque();

    public void addWorker(Worker<N> w) {
        if (!this.workers.contains(w)) {
            this.workers.add(w);
        }
    }

    public void removeWorker(Worker<N> w) {
        if (this.workers.contains(w)) {
            this.workers.remove(w);
        }
    }

    public void queueData(N n) {
        this.worklist.add(n);
    }

    public void queueData(Collection<N> ns) {
        this.worklist.addAll(ns);
    }

    public void processQueue() {
        while (!this.worklist.isEmpty()) {
            N m = this.worklist.removeFirst();
            if (this.processed.contains(m)) continue;
            this.process(m);
            this.processed.add(m);
        }
    }

    protected void process(N n) {
        for (Worker<N> w : this.workers) {
            w.process(this, n);
        }
    }

    public boolean hasProcessed(N n) {
        return this.processed.contains(n);
    }

    public int pending() {
        return this.worklist.size();
    }

    public static interface Worker<N> {
        public void process(Worklist<N> var1, N var2);
    }
}

