/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.mapleir.stdlib.collections.map.ValueCreator;

public class CollectionUtils {
    public static <T, K> Map<T, K> copyOf(Map<T, K> src) {
        HashMap dst = new HashMap();
        CollectionUtils.copy(src, dst);
        return dst;
    }

    public static <T, K> void copy(Map<T, K> src, Map<T, K> dst) {
        for (Map.Entry<T, K> e : src.entrySet()) {
            dst.put(e.getKey(), e.getValue());
        }
    }

    public static <T> List<T> collate(Iterator<T> it) {
        ArrayList<T> list = new ArrayList<T>();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }

    @SafeVarargs
    public static <E, C extends Collection<E>> C asCollection(ValueCreator<C> vc, E ... elements) {
        Collection col = (Collection)vc.create();
        if (elements != null) {
            for (E e : elements) {
                if (e == null) continue;
                col.add(e);
            }
        }
        return (C)col;
    }
}

