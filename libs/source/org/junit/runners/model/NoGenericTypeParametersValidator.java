/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners.model;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
class NoGenericTypeParametersValidator {
    private final Method method;

    NoGenericTypeParametersValidator(Method method) {
        this.method = method;
    }

    void validate(List<Throwable> errors) {
        for (Type each : this.method.getGenericParameterTypes()) {
            this.validateNoTypeParameterOnType(each, errors);
        }
    }

    private void validateNoTypeParameterOnType(Type type, List<Throwable> errors) {
        if (type instanceof TypeVariable) {
            errors.add(new Exception("Method " + this.method.getName() + "() contains unresolved type variable " + type));
        } else if (type instanceof ParameterizedType) {
            this.validateNoTypeParameterOnParameterizedType((ParameterizedType)type, errors);
        } else if (type instanceof WildcardType) {
            this.validateNoTypeParameterOnWildcardType((WildcardType)type, errors);
        } else if (type instanceof GenericArrayType) {
            this.validateNoTypeParameterOnGenericArrayType((GenericArrayType)type, errors);
        }
    }

    private void validateNoTypeParameterOnParameterizedType(ParameterizedType parameterized, List<Throwable> errors) {
        for (Type each : parameterized.getActualTypeArguments()) {
            this.validateNoTypeParameterOnType(each, errors);
        }
    }

    private void validateNoTypeParameterOnWildcardType(WildcardType wildcard, List<Throwable> errors) {
        for (Type each : wildcard.getUpperBounds()) {
            this.validateNoTypeParameterOnType(each, errors);
        }
        for (Type each : wildcard.getLowerBounds()) {
            this.validateNoTypeParameterOnType(each, errors);
        }
    }

    private void validateNoTypeParameterOnGenericArrayType(GenericArrayType arrayType, List<Throwable> errors) {
        this.validateNoTypeParameterOnType(arrayType.getGenericComponentType(), errors);
    }
}

