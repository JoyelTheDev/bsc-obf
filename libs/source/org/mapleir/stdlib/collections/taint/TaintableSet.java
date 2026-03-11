/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.taint;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.mapleir.stdlib.collections.itertools.ProductIterator;
import org.mapleir.stdlib.collections.taint.ITaintable;
import org.mapleir.stdlib.util.Pair;

public class TaintableSet<T>
implements Set<T>,
ITaintable {
    private final Set<T> backingSet;
    private boolean tainted;

    public TaintableSet(boolean dirty) {
        this();
        this.tainted = dirty;
    }

    public TaintableSet() {
        this.backingSet = new HashSet<T>();
        this.tainted = false;
    }

    public TaintableSet(TaintableSet<T> other) {
        this.backingSet = new HashSet<T>(other);
        this.tainted = other.tainted;
    }

    public Iterator<Pair<T, T>> product(TaintableSet<T> other) {
        return ProductIterator.getIterator(this, other);
    }

    public void taint() {
        this.tainted = true;
    }

    @Override
    public boolean isTainted() {
        return this.tainted;
    }

    @Override
    public boolean union(ITaintable t) {
        if (t instanceof Collection) {
            this.backingSet.addAll((Collection)((Object)t));
        } else if (t != null) {
            this.backingSet.add(t);
        }
        return this.tainted |= t.isTainted();
    }

    @Override
    public int size() {
        return this.backingSet.size();
    }

    @Override
    public boolean isEmpty() {
        return this.backingSet.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.backingSet.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return this.backingSet.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.backingSet.toArray();
    }

    @Override
    public <T2> T2[] toArray(T2[] a) {
        return this.backingSet.toArray(a);
    }

    @Override
    public boolean add(T e) {
        return this.backingSet.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return this.backingSet.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if (c instanceof TaintableSet) {
            TaintableSet ts = (TaintableSet)c;
            if (ts.isTainted()) {
                return false;
            }
            return this.backingSet.containsAll(ts.backingSet);
        }
        return this.backingSet.containsAll(c);
    }

    @Override
    @Deprecated
    public boolean addAll(Collection<? extends T> c) {
        return this.backingSet.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.backingSet.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this.backingSet.removeAll(c);
    }

    @Override
    public void clear() {
        this.backingSet.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        TaintableSet that = (TaintableSet)o;
        if (this.tainted != that.tainted) {
            return false;
        }
        return this.backingSet.equals(that.backingSet);
    }

    @Override
    public int hashCode() {
        int result = this.backingSet.hashCode();
        result = 31 * result + (this.tainted ? 1 : 0);
        return result;
    }

    public String toString() {
        return this.backingSet.toString() + " (tainted=" + this.tainted + ")";
    }
}

