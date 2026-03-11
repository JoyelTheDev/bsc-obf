/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.categories;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.CategoryFilterFactory;
import org.junit.runner.manipulation.Filter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public final class ExcludeCategories
extends CategoryFilterFactory {
    @Override
    protected Filter createFilter(List<Class<?>> categories) {
        return new ExcludesAny(categories);
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class ExcludesAny
    extends Categories.CategoryFilter {
        public ExcludesAny(List<Class<?>> categories) {
            this(new HashSet(categories));
        }

        public ExcludesAny(Set<Class<?>> categories) {
            super(true, null, true, categories);
        }

        @Override
        public String describe() {
            return "excludes " + super.describe();
        }
    }
}

