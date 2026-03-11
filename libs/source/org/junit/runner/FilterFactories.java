/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runner;

import org.junit.internal.Classes;
import org.junit.runner.Description;
import org.junit.runner.FilterFactory;
import org.junit.runner.FilterFactoryParams;
import org.junit.runner.Request;
import org.junit.runner.manipulation.Filter;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
class FilterFactories {
    FilterFactories() {
    }

    public static Filter createFilterFromFilterSpec(Request request, String filterSpec) throws FilterFactory.FilterNotCreatedException {
        Description topLevelDescription = request.getRunner().getDescription();
        String[] tuple = filterSpec.contains("=") ? filterSpec.split("=", 2) : new String[]{filterSpec, ""};
        return FilterFactories.createFilter(tuple[0], new FilterFactoryParams(topLevelDescription, tuple[1]));
    }

    public static Filter createFilter(String filterFactoryFqcn, FilterFactoryParams params) throws FilterFactory.FilterNotCreatedException {
        FilterFactory filterFactory = FilterFactories.createFilterFactory(filterFactoryFqcn);
        return filterFactory.createFilter(params);
    }

    public static Filter createFilter(Class<? extends FilterFactory> filterFactoryClass, FilterFactoryParams params) throws FilterFactory.FilterNotCreatedException {
        FilterFactory filterFactory = FilterFactories.createFilterFactory(filterFactoryClass);
        return filterFactory.createFilter(params);
    }

    static FilterFactory createFilterFactory(String filterFactoryFqcn) throws FilterFactory.FilterNotCreatedException {
        Class<FilterFactory> filterFactoryClass;
        try {
            filterFactoryClass = Classes.getClass(filterFactoryFqcn).asSubclass(FilterFactory.class);
        }
        catch (Exception e) {
            throw new FilterFactory.FilterNotCreatedException(e);
        }
        return FilterFactories.createFilterFactory(filterFactoryClass);
    }

    static FilterFactory createFilterFactory(Class<? extends FilterFactory> filterFactoryClass) throws FilterFactory.FilterNotCreatedException {
        try {
            return filterFactoryClass.getConstructor(new Class[0]).newInstance(new Object[0]);
        }
        catch (Exception e) {
            throw new FilterFactory.FilterNotCreatedException(e);
        }
    }
}

