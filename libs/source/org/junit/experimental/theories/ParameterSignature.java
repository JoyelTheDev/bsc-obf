/*
 * Decompiled with CFR 0.152.
 */
package org.junit.experimental.theories;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class ParameterSignature {
    private static final Map<Class<?>, Class<?>> CONVERTABLE_TYPES_MAP = ParameterSignature.buildConvertableTypesMap();
    private final Class<?> type;
    private final Annotation[] annotations;

    private static Map<Class<?>, Class<?>> buildConvertableTypesMap() {
        HashMap map = new HashMap();
        ParameterSignature.putSymmetrically(map, Boolean.TYPE, Boolean.class);
        ParameterSignature.putSymmetrically(map, Byte.TYPE, Byte.class);
        ParameterSignature.putSymmetrically(map, Short.TYPE, Short.class);
        ParameterSignature.putSymmetrically(map, Character.TYPE, Character.class);
        ParameterSignature.putSymmetrically(map, Integer.TYPE, Integer.class);
        ParameterSignature.putSymmetrically(map, Long.TYPE, Long.class);
        ParameterSignature.putSymmetrically(map, Float.TYPE, Float.class);
        ParameterSignature.putSymmetrically(map, Double.TYPE, Double.class);
        return Collections.unmodifiableMap(map);
    }

    private static <T> void putSymmetrically(Map<T, T> map, T a, T b) {
        map.put(a, b);
        map.put(b, a);
    }

    public static ArrayList<ParameterSignature> signatures(Method method) {
        return ParameterSignature.signatures(method.getParameterTypes(), method.getParameterAnnotations());
    }

    public static List<ParameterSignature> signatures(Constructor<?> constructor) {
        return ParameterSignature.signatures(constructor.getParameterTypes(), constructor.getParameterAnnotations());
    }

    private static ArrayList<ParameterSignature> signatures(Class<?>[] parameterTypes, Annotation[][] parameterAnnotations) {
        ArrayList<ParameterSignature> sigs = new ArrayList<ParameterSignature>();
        for (int i = 0; i < parameterTypes.length; ++i) {
            sigs.add(new ParameterSignature(parameterTypes[i], parameterAnnotations[i]));
        }
        return sigs;
    }

    private ParameterSignature(Class<?> type, Annotation[] annotations) {
        this.type = type;
        this.annotations = annotations;
    }

    public boolean canAcceptValue(Object candidate) {
        return candidate == null ? !this.type.isPrimitive() : this.canAcceptType(candidate.getClass());
    }

    public boolean canAcceptType(Class<?> candidate) {
        return this.type.isAssignableFrom(candidate) || this.isAssignableViaTypeConversion(this.type, candidate);
    }

    public boolean canPotentiallyAcceptType(Class<?> candidate) {
        return candidate.isAssignableFrom(this.type) || this.isAssignableViaTypeConversion(candidate, this.type) || this.canAcceptType(candidate);
    }

    private boolean isAssignableViaTypeConversion(Class<?> targetType, Class<?> candidate) {
        if (CONVERTABLE_TYPES_MAP.containsKey(candidate)) {
            Class<?> wrapperClass = CONVERTABLE_TYPES_MAP.get(candidate);
            return targetType.isAssignableFrom(wrapperClass);
        }
        return false;
    }

    public Class<?> getType() {
        return this.type;
    }

    public List<Annotation> getAnnotations() {
        return Arrays.asList(this.annotations);
    }

    public boolean hasAnnotation(Class<? extends Annotation> type) {
        return this.getAnnotation(type) != null;
    }

    public <T extends Annotation> T findDeepAnnotation(Class<T> annotationType) {
        Annotation[] annotations2 = this.annotations;
        return this.findDeepAnnotation(annotations2, annotationType, 3);
    }

    private <T extends Annotation> T findDeepAnnotation(Annotation[] annotations, Class<T> annotationType, int depth) {
        if (depth == 0) {
            return null;
        }
        for (Annotation each : annotations) {
            if (annotationType.isInstance(each)) {
                return (T)((Annotation)annotationType.cast(each));
            }
            T candidate = this.findDeepAnnotation(each.annotationType().getAnnotations(), annotationType, depth - 1);
            if (candidate == null) continue;
            return (T)((Annotation)annotationType.cast(candidate));
        }
        return null;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        for (Annotation each : this.getAnnotations()) {
            if (!annotationType.isInstance(each)) continue;
            return (T)((Annotation)annotationType.cast(each));
        }
        return null;
    }
}

