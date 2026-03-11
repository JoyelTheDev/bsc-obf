/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.map;

import java.util.ArrayList;
import java.util.List;
import org.mapleir.stdlib.collections.map.ValueCreator;

public class ListCreator<T>
implements ValueCreator<List<T>> {
    @Override
    public List<T> create() {
        return new ArrayList();
    }
}

