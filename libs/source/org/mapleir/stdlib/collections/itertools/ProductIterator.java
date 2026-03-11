/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.itertools;

import java.util.Iterator;
import org.mapleir.stdlib.util.Pair;

public abstract class ProductIterator<T>
implements Iterator<Pair<T, T>> {
    public static <T> ProductIterator<T> getIterator(Iterable<T> a, Iterable<T> b) {
        Iterator<T> iteratorB;
        Iterator<T> iteratorA = a.iterator();
        if (iteratorA.hasNext() && (iteratorB = b.iterator()).hasNext()) {
            return new BasicProductIterator<T>(a, b);
        }
        return new EmptyProductIterator();
    }

    @Override
    public abstract Pair<T, T> next();

    static class BasicProductIterator<T>
    extends ProductIterator<T> {
        private Iterator<T> iteratorA;
        private Iterator<T> iteratorB;
        private T curA;
        private Iterable<T> b;

        public BasicProductIterator(Iterable<T> a, Iterable<T> b) {
            this.iteratorA = a.iterator();
            this.iteratorB = b.iterator();
            this.curA = null;
            this.b = b;
        }

        @Override
        public boolean hasNext() {
            if (!this.iteratorB.hasNext()) {
                if (!this.iteratorA.hasNext()) {
                    return false;
                }
                this.curA = this.iteratorA.next();
                this.iteratorB = this.b.iterator();
            }
            return true;
        }

        @Override
        public Pair<T, T> next() {
            if (this.curA == null) {
                this.curA = this.iteratorA.next();
            }
            return new Pair<T, T>(this.curA, this.iteratorB.next());
        }
    }

    static class EmptyProductIterator<T>
    extends ProductIterator<T> {
        EmptyProductIterator() {
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Pair<T, T> next() {
            throw new UnsupportedOperationException();
        }
    }
}

