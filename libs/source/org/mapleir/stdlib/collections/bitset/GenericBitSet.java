/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.bitset;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import org.mapleir.stdlib.collections.bitset.BitSetIndexer;

public class GenericBitSet<N>
implements Set<N> {
    private BitSet bitset;
    private BitSetIndexer<N> indexer;

    public GenericBitSet(BitSetIndexer<N> indexer) {
        this.bitset = new BitSet();
        this.indexer = indexer;
    }

    public GenericBitSet(GenericBitSet<N> other) {
        this.indexer = other.indexer;
        this.bitset = (BitSet)other.bitset.clone();
    }

    public GenericBitSet<N> copy() {
        return new GenericBitSet<N>(this);
    }

    public boolean set(N n, boolean state) {
        if (n == null) {
            throw new IllegalArgumentException();
        }
        if (!state && !this.indexer.isIndexed(n)) {
            return false;
        }
        int index = this.indexer.getIndex(n);
        boolean ret = this.bitset.get(index);
        this.bitset.set(index, state);
        return ret;
    }

    @Override
    public boolean add(N n) {
        boolean ret;
        if (n == null) {
            throw new IllegalArgumentException();
        }
        boolean bl = ret = !this.contains(n);
        if (n != null && this.indexer.getIndex(n) > 100000) {
            System.err.println("Probable bitset memory leak");
            System.err.println(this.indexer.getIndex(n) + " " + n.getClass().getName());
            System.err.println(this.indexer.getClass().getName());
            new Throwable().printStackTrace();
        }
        this.bitset.set(this.indexer.getIndex(n));
        return ret;
    }

    @Override
    public boolean remove(Object o) {
        if (!this.contains(o)) {
            return false;
        }
        this.bitset.set(this.indexer.getIndex(o), false);
        return true;
    }

    @Override
    public boolean containsAll(GenericBitSet<N> other) {
        BitSet temp = (BitSet)other.bitset.clone();
        temp.and(this.bitset);
        return temp.equals(other.bitset);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (this.contains(o)) continue;
            return false;
        }
        return true;
    }

    public boolean containsNone(GenericBitSet<N> other) {
        BitSet temp = (BitSet)this.bitset.clone();
        temp.and(other.bitset);
        return temp.isEmpty();
    }

    public boolean containsAny(GenericBitSet<N> other) {
        return !this.containsNone(other);
    }

    public void addAll(GenericBitSet<N> n) {
        if (this.indexer != n.indexer) {
            throw new IllegalArgumentException("Fast addAll operands must share the same BitSetIndexer");
        }
        this.bitset.or(n.bitset);
    }

    public GenericBitSet<N> union(GenericBitSet<N> other) {
        GenericBitSet<N> copy = this.copy();
        copy.addAll(other);
        return copy;
    }

    @Override
    public boolean addAll(Collection<? extends N> c) {
        boolean ret = false;
        for (N o : c) {
            ret = this.add(o) || ret;
        }
        return ret;
    }

    public void retainAll(GenericBitSet<N> other) {
        this.bitset.and(other.bitset);
    }

    public GenericBitSet<N> intersect(GenericBitSet<N> other) {
        GenericBitSet<N> copy = this.copy();
        copy.retainAll(other);
        return copy;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean ret = false;
        Iterator<N> it = this.iterator();
        while (it.hasNext()) {
            if (c.contains(it.next())) continue;
            it.remove();
            ret = true;
        }
        return ret;
    }

    public void removeAll(GenericBitSet<N> other) {
        this.bitset.andNot(other.bitset);
    }

    public GenericBitSet<N> relativeComplement(GenericBitSet<N> other) {
        GenericBitSet<N> copy = this.copy();
        copy.removeAll(other);
        return copy;
    }

    public GenericBitSet<N> relativeComplement(N n) {
        GenericBitSet<N> copy = this.copy();
        copy.remove(n);
        return copy;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean ret = false;
        for (Object o : c) {
            ret = this.remove(o) || ret;
        }
        return ret;
    }

    @Override
    public void clear() {
        this.bitset.clear();
    }

    @Override
    public int size() {
        return this.bitset.cardinality();
    }

    @Override
    public boolean isEmpty() {
        return this.bitset.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            throw new IllegalArgumentException();
        }
        return this.indexer.isIndexed(o) && this.bitset.get(this.indexer.getIndex(o));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("[ ");
        for (N n : this) {
            sb.append(n).append(" ");
        }
        return sb.append("]").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GenericBitSet)) {
            return false;
        }
        GenericBitSet gbs = (GenericBitSet)o;
        return this.indexer == gbs.indexer && this.bitset.equals(gbs.bitset);
    }

    @Override
    public Iterator<N> iterator() {
        return new Iterator<N>(){
            int index = -1;

            @Override
            public boolean hasNext() {
                return GenericBitSet.this.bitset.nextSetBit(this.index + 1) != -1;
            }

            @Override
            public N next() {
                this.index = GenericBitSet.this.bitset.nextSetBit(this.index + 1);
                return GenericBitSet.this.indexer.get(this.index);
            }

            @Override
            public void remove() {
                GenericBitSet.this.bitset.set(this.index, false);
            }
        };
    }

    @Override
    public Spliterator<N> spliterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }
}

