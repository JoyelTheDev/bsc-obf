/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.list;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class NotifiedList<E>
extends ArrayList<E>
implements Serializable {
    private final Consumer<E> onAdded;
    private final Consumer<E> onRemoved;

    public NotifiedList(Consumer<E> onAdded, Consumer<E> onRemoved) {
        this.onAdded = onAdded;
        this.onRemoved = onRemoved;
    }

    @Override
    public boolean add(E elem) {
        if (elem != null) {
            this.onAdded.accept(elem);
        }
        return super.add(elem);
    }

    @Override
    public void add(int index, E elem) {
        if (elem != null) {
            this.onAdded.accept(elem);
        }
        super.add(index, elem);
    }

    @Override
    public boolean remove(Object o) {
        boolean ret = super.remove(o);
        if (ret && o != null) {
            this.onRemoved.accept(o);
        }
        return ret;
    }

    @Override
    public E remove(int index) {
        Object oldElem = super.remove(index);
        if (oldElem != null) {
            this.onRemoved.accept(oldElem);
        }
        return oldElem;
    }

    @Override
    public E set(int index, E elem) {
        E oldElem;
        if (elem != null) {
            this.onAdded.accept(elem);
        }
        if ((oldElem = super.set(index, elem)) != null) {
            this.onRemoved.accept(oldElem);
        }
        return oldElem;
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        for (int i = fromIndex; i < toIndex; ++i) {
            Object elem = this.get(i);
            if (elem == null) continue;
            this.onRemoved.accept(elem);
        }
        super.removeRange(fromIndex, toIndex);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        for (E elem : c) {
            this.add(elem);
        }
        return c.size() != 0;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        for (E elem : c) {
            this.add(index++, elem);
        }
        return c.size() != 0;
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
    public boolean retainAll(Collection<?> c) {
        boolean ret = false;
        Iterator it = this.iterator();
        while (it.hasNext()) {
            Object elem = it.next();
            if (c.contains(elem)) continue;
            it.remove();
            ret = true;
        }
        return ret;
    }

    @Override
    public void clear() {
        Iterator it = this.iterator();
        while (it.hasNext()) {
            Object s = it.next();
            it.remove();
        }
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        throw new UnsupportedOperationException();
    }

    private void readObject(ObjectInputStream s) {
        throw new UnsupportedOperationException();
    }

    private void writeObject(ObjectOutputStream s) {
        throw new UnsupportedOperationException();
    }
}

