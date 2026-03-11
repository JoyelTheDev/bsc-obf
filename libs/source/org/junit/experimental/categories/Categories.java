/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.categories;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.experimental.categories.Category;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class Categories
extends Suite {
    public Categories(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        super(klass, builder);
        try {
            Set<Class<?>> included = Categories.getIncludedCategory(klass);
            Set<Class<?>> excluded = Categories.getExcludedCategory(klass);
            boolean isAnyIncluded = Categories.isAnyIncluded(klass);
            boolean isAnyExcluded = Categories.isAnyExcluded(klass);
            this.filter(CategoryFilter.categoryFilter(isAnyIncluded, included, isAnyExcluded, excluded));
        }
        catch (NoTestsRemainException e) {
            throw new InitializationError(e);
        }
        Categories.assertNoCategorizedDescendentsOfUncategorizeableParents(this.getDescription());
    }

    private static Set<Class<?>> getIncludedCategory(Class<?> klass) {
        IncludeCategory annotation = klass.getAnnotation(IncludeCategory.class);
        return Categories.createSet(annotation == null ? null : annotation.value());
    }

    private static boolean isAnyIncluded(Class<?> klass) {
        IncludeCategory annotation = klass.getAnnotation(IncludeCategory.class);
        return annotation == null || annotation.matchAny();
    }

    private static Set<Class<?>> getExcludedCategory(Class<?> klass) {
        ExcludeCategory annotation = klass.getAnnotation(ExcludeCategory.class);
        return Categories.createSet(annotation == null ? null : annotation.value());
    }

    private static boolean isAnyExcluded(Class<?> klass) {
        ExcludeCategory annotation = klass.getAnnotation(ExcludeCategory.class);
        return annotation == null || annotation.matchAny();
    }

    private static void assertNoCategorizedDescendentsOfUncategorizeableParents(Description description) throws InitializationError {
        if (!Categories.canHaveCategorizedChildren(description)) {
            Categories.assertNoDescendantsHaveCategoryAnnotations(description);
        }
        for (Description each : description.getChildren()) {
            Categories.assertNoCategorizedDescendentsOfUncategorizeableParents(each);
        }
    }

    private static void assertNoDescendantsHaveCategoryAnnotations(Description description) throws InitializationError {
        for (Description each : description.getChildren()) {
            if (each.getAnnotation(Category.class) != null) {
                throw new InitializationError("Category annotations on Parameterized classes are not supported on individual methods.");
            }
            Categories.assertNoDescendantsHaveCategoryAnnotations(each);
        }
    }

    private static boolean canHaveCategorizedChildren(Description description) {
        for (Description each : description.getChildren()) {
            if (each.getTestClass() != null) continue;
            return false;
        }
        return true;
    }

    private static boolean hasAssignableTo(Set<Class<?>> assigns, Class<?> to) {
        for (Class<?> from : assigns) {
            if (!to.isAssignableFrom(from)) continue;
            return true;
        }
        return false;
    }

    private static Set<Class<?>> createSet(Class<?> ... t) {
        HashSet set = new HashSet();
        if (t != null) {
            Collections.addAll(set, t);
        }
        return set;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static class CategoryFilter
    extends Filter {
        private final Set<Class<?>> included;
        private final Set<Class<?>> excluded;
        private final boolean includedAny;
        private final boolean excludedAny;

        public static CategoryFilter include(boolean matchAny, Class<?> ... categories) {
            if (CategoryFilter.hasNull(categories)) {
                throw new NullPointerException("has null category");
            }
            return CategoryFilter.categoryFilter(matchAny, Categories.createSet(categories), true, null);
        }

        public static CategoryFilter include(Class<?> category) {
            return CategoryFilter.include(true, category);
        }

        public static CategoryFilter include(Class<?> ... categories) {
            return CategoryFilter.include(true, categories);
        }

        public static CategoryFilter exclude(boolean matchAny, Class<?> ... categories) {
            if (CategoryFilter.hasNull(categories)) {
                throw new NullPointerException("has null category");
            }
            return CategoryFilter.categoryFilter(true, null, matchAny, Categories.createSet(categories));
        }

        public static CategoryFilter exclude(Class<?> category) {
            return CategoryFilter.exclude(true, category);
        }

        public static CategoryFilter exclude(Class<?> ... categories) {
            return CategoryFilter.exclude(true, categories);
        }

        public static CategoryFilter categoryFilter(boolean matchAnyInclusions, Set<Class<?>> inclusions, boolean matchAnyExclusions, Set<Class<?>> exclusions) {
            return new CategoryFilter(matchAnyInclusions, inclusions, matchAnyExclusions, exclusions);
        }

        protected CategoryFilter(boolean matchAnyIncludes, Set<Class<?>> includes, boolean matchAnyExcludes, Set<Class<?>> excludes) {
            this.includedAny = matchAnyIncludes;
            this.excludedAny = matchAnyExcludes;
            this.included = CategoryFilter.copyAndRefine(includes);
            this.excluded = CategoryFilter.copyAndRefine(excludes);
        }

        @Override
        public String describe() {
            return this.toString();
        }

        public String toString() {
            StringBuilder description = new StringBuilder("categories ").append(this.included.isEmpty() ? "[all]" : this.included);
            if (!this.excluded.isEmpty()) {
                description.append(" - ").append(this.excluded);
            }
            return description.toString();
        }

        @Override
        public boolean shouldRun(Description description) {
            if (this.hasCorrectCategoryAnnotation(description)) {
                return true;
            }
            for (Description each : description.getChildren()) {
                if (!this.shouldRun(each)) continue;
                return true;
            }
            return false;
        }

        private boolean hasCorrectCategoryAnnotation(Description description) {
            Set<Class<?>> childCategories = CategoryFilter.categories(description);
            if (childCategories.isEmpty()) {
                return this.included.isEmpty();
            }
            if (!this.excluded.isEmpty() && (this.excludedAny ? this.matchesAnyParentCategories(childCategories, this.excluded) : this.matchesAllParentCategories(childCategories, this.excluded))) {
                return false;
            }
            if (this.included.isEmpty()) {
                return true;
            }
            if (this.includedAny) {
                return this.matchesAnyParentCategories(childCategories, this.included);
            }
            return this.matchesAllParentCategories(childCategories, this.included);
        }

        private boolean matchesAnyParentCategories(Set<Class<?>> childCategories, Set<Class<?>> parentCategories) {
            for (Class<?> parentCategory : parentCategories) {
                if (!Categories.hasAssignableTo(childCategories, parentCategory)) continue;
                return true;
            }
            return false;
        }

        private boolean matchesAllParentCategories(Set<Class<?>> childCategories, Set<Class<?>> parentCategories) {
            for (Class<?> parentCategory : parentCategories) {
                if (Categories.hasAssignableTo(childCategories, parentCategory)) continue;
                return false;
            }
            return true;
        }

        private static Set<Class<?>> categories(Description description) {
            HashSet categories = new HashSet();
            Collections.addAll(categories, CategoryFilter.directCategories(description));
            Collections.addAll(categories, CategoryFilter.directCategories(CategoryFilter.parentDescription(description)));
            return categories;
        }

        private static Description parentDescription(Description description) {
            Class<?> testClass = description.getTestClass();
            return testClass == null ? null : Description.createSuiteDescription(testClass);
        }

        private static Class<?>[] directCategories(Description description) {
            if (description == null) {
                return new Class[0];
            }
            Category annotation = description.getAnnotation(Category.class);
            return annotation == null ? new Class[]{} : annotation.value();
        }

        private static Set<Class<?>> copyAndRefine(Set<Class<?>> classes) {
            HashSet c = new HashSet();
            if (classes != null) {
                c.addAll(classes);
            }
            c.remove(null);
            return c;
        }

        private static boolean hasNull(Class<?> ... classes) {
            if (classes == null) {
                return false;
            }
            for (Class<?> clazz : classes) {
                if (clazz != null) continue;
                return true;
            }
            return false;
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    @Retention(value=RetentionPolicy.RUNTIME)
    public static @interface ExcludeCategory {
        public Class<?>[] value() default {};

        public boolean matchAny() default true;
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    @Retention(value=RetentionPolicy.RUNTIME)
    public static @interface IncludeCategory {
        public Class<?>[] value() default {};

        public boolean matchAny() default true;
    }
}

