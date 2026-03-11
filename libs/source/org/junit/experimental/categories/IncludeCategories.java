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
public final class IncludeCategories
extends CategoryFilterFactory {
    @Override
    protected Filter createFilter(List<Class<?>> categories) {
        return new IncludesAny(categories);
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static class IncludesAny
    extends Categories.CategoryFilter {
        public IncludesAny(List<Class<?>> categories) {
            this(new HashSet(categories));
        }

        public IncludesAny(Set<Class<?>> categories) {
            super(true, categories, true, null);
        }

        @Override
        public String describe() {
            return "includes " + super.describe();
        }
    }
}

