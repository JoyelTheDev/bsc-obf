/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.itertools;

import java.util.Collection;
import java.util.Iterator;

public abstract class ChainIterator<T>
implements Iterator<T> {
    protected Iterator<T> current;

    private boolean findNext() {
        Iterator<T> nextIt = this.nextIterator();
        if (nextIt == null) {
            return false;
        }
        this.current = nextIt;
        return this.hasNext(nextIt);
    }

    private boolean hasNext(Iterator<T> it) {
        if (it == null) {
            return this.findNext();
        }
        if (it.hasNext()) {
            return true;
        }
        return this.findNext();
    }

    @Override
    public boolean hasNext() {
        return this.hasNext(this.current);
    }

    @Override
    public T next() {
        return this.current.next();
    }

    public abstract Iterator<T> nextIterator();

    public static class CollectionChainIterator<T>
    extends ChainIterator<T> {
        private final Iterator<? extends Collection<T>> it;

        public CollectionChainIterator(Collection<? extends Collection<T>> collections) {
            this.it = collections.iterator();
        }

        @Override
        public Iterator<T> nextIterator() {
            if (this.it.hasNext()) {
                return this.it.next().iterator();
            }
            return null;
        }
    }
}

