/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.mapleir.stdlib.collections.list.NotifiedList;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;

public class IndexedList<T>
implements List<T> {
    private NullPermeableHashMap<T, List<Integer>> indexMap;
    private List<T> backingList = new NotifiedList<Object>(t -> {
        this.dirty = true;
    }, t -> {
        this.dirty = true;
    });
    private boolean dirty = false;

    public IndexedList() {
        this.indexMap = new NullPermeableHashMap(ArrayList::new);
    }

    public IndexedList(Collection<T> other) {
        this();
        this.addAll(other);
    }

    private void recacheIndices() {
        if (!this.dirty) {
            return;
        }
        this.indexMap.clear();
        for (int i = 0; i < this.backingList.size(); ++i) {
            this.indexMap.getNonNull(this.backingList.get(i)).add(i);
        }
        this.dirty = false;
    }

    @Override
    public int size() {
        return this.backingList.size();
    }

    @Override
    public boolean isEmpty() {
        return this.backingList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        this.recacheIndices();
        return this.indexMap.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return this.backingList.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.backingList.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return this.backingList.toArray(a);
    }

    @Override
    public boolean add(T t) {
        boolean ret = this.backingList.add(t);
        this.dirty |= ret;
        return ret;
    }

    @Override
    public boolean remove(Object o) {
        boolean ret = this.backingList.remove(o);
        this.dirty |= ret;
        return ret;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return this.backingList.contains(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean ret = this.backingList.addAll(c);
        this.dirty |= ret;
        return ret;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        boolean ret = this.backingList.addAll(index, c);
        this.dirty |= ret;
        return ret;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean ret = this.backingList.removeAll(c);
        this.dirty |= ret;
        return ret;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean ret = this.backingList.retainAll(c);
        this.dirty |= ret;
        return ret;
    }

    @Override
    public void clear() {
        this.backingList.clear();
        this.indexMap.clear();
        this.dirty = false;
    }

    @Override
    public T get(int index) {
        return this.backingList.get(index);
    }

    @Override
    public T set(int index, T element) {
        T prev = this.backingList.set(index, element);
        this.dirty |= prev != element;
        return prev;
    }

    @Override
    public void add(int index, T element) {
        this.backingList.add(index, element);
        this.dirty = true;
    }

    @Override
    public T remove(int index) {
        T ret = this.backingList.remove(index);
        this.dirty = true;
        return ret;
    }

    @Override
    public int indexOf(Object o) {
        this.recacheIndices();
        List indices = (List)this.indexMap.get(o);
        return indices == null ? -1 : (Integer)indices.get(0);
    }

    @Override
    public int lastIndexOf(Object o) {
        this.recacheIndices();
        List indices = (List)this.indexMap.get(o);
        return indices == null ? -1 : (Integer)indices.get(indices.size() - 1);
    }

    @Override
    public ListIterator<T> listIterator() {
        return this.backingList.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return this.backingList.listIterator();
    }

    @Override
    public IndexedList<T> subList(int fromIndex, int toIndex) {
        IndexedList<T> sub = new IndexedList<T>();
        sub.backingList = this.backingList.subList(fromIndex, toIndex);
        sub.dirty = true;
        super.recacheIndices();
        return sub;
    }

    public String toString() {
        return this.backingList.toString();
    }
}

