/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runner.manipulation;

import java.util.Comparator;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Sortable;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Sorter
implements Comparator<Description> {
    public static final Sorter NULL = new Sorter(new Comparator<Description>(){

        @Override
        public int compare(Description o1, Description o2) {
            return 0;
        }
    });
    private final Comparator<Description> comparator;

    public Sorter(Comparator<Description> comparator) {
        this.comparator = comparator;
    }

    public void apply(Object object) {
        if (object instanceof Sortable) {
            Sortable sortable = (Sortable)object;
            sortable.sort(this);
        }
    }

    @Override
    public int compare(Description o1, Description o2) {
        return this.comparator.compare(o1, o2);
    }
}

