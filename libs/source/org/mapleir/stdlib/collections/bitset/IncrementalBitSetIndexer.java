/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.collections.bitset;

import java.util.HashMap;
import org.mapleir.stdlib.collections.bitset.BitSetIndexer;
import org.mapleir.stdlib.collections.map.NullPermeableHashMap;
import org.mapleir.stdlib.collections.map.ValueCreator;

public class IncrementalBitSetIndexer<N>
implements BitSetIndexer<N> {
    private final NullPermeableHashMap<N, Integer> map = new NullPermeableHashMap(new ValueCreator<Integer>(){

        @Override
        public Integer create() {
            return IncrementalBitSetIndexer.this.map.size() + 1;
        }
    });
    private final HashMap<Integer, N> reverseMap = new HashMap();

    @Override
    public int getIndex(N n) {
        int index = this.map.getNonNull(n);
        this.reverseMap.put(index, n);
        return index;
    }

    @Override
    public N get(int index) {
        return this.reverseMap.get(index);
    }

    @Override
    public boolean isIndexed(N o) {
        return this.map.containsKey(o);
    }
}

