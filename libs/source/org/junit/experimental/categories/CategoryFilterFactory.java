/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.categories;

import java.util.ArrayList;
import java.util.List;
import org.junit.internal.Classes;
import org.junit.runner.FilterFactory;
import org.junit.runner.FilterFactoryParams;
import org.junit.runner.manipulation.Filter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
abstract class CategoryFilterFactory
implements FilterFactory {
    CategoryFilterFactory() {
    }

    @Override
    public Filter createFilter(FilterFactoryParams params) throws FilterFactory.FilterNotCreatedException {
        try {
            return this.createFilter(this.parseCategories(params.getArgs()));
        }
        catch (ClassNotFoundException e) {
            throw new FilterFactory.FilterNotCreatedException(e);
        }
    }

    protected abstract Filter createFilter(List<Class<?>> var1);

    private List<Class<?>> parseCategories(String categories) throws ClassNotFoundException {
        ArrayList categoryClasses = new ArrayList();
        for (String category : categories.split(",")) {
            Class<?> categoryClass = Classes.getClass(category);
            categoryClasses.add(categoryClass);
        }
        return categoryClasses;
    }
}

