/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runners.model.FrameworkMember;
import org.junit.runners.model.NoGenericTypeParametersValidator;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class FrameworkMethod
extends FrameworkMember<FrameworkMethod> {
    private final Method method;

    public FrameworkMethod(Method method) {
        if (method == null) {
            throw new NullPointerException("FrameworkMethod cannot be created without an underlying method.");
        }
        this.method = method;
    }

    public Method getMethod() {
        return this.method;
    }

    public Object invokeExplosively(final Object target, final Object ... params) throws Throwable {
        return new ReflectiveCallable(){

            protected Object runReflectiveCall() throws Throwable {
                return FrameworkMethod.this.method.invoke(target, params);
            }
        }.run();
    }

    @Override
    public String getName() {
        return this.method.getName();
    }

    public void validatePublicVoidNoArg(boolean isStatic, List<Throwable> errors) {
        this.validatePublicVoid(isStatic, errors);
        if (this.method.getParameterTypes().length != 0) {
            errors.add(new Exception("Method " + this.method.getName() + " should have no parameters"));
        }
    }

    public void validatePublicVoid(boolean isStatic, List<Throwable> errors) {
        if (this.isStatic() != isStatic) {
            String state = isStatic ? "should" : "should not";
            errors.add(new Exception("Method " + this.method.getName() + "() " + state + " be static"));
        }
        if (!this.isPublic()) {
            errors.add(new Exception("Method " + this.method.getName() + "() should be public"));
        }
        if (this.method.getReturnType() != Void.TYPE) {
            errors.add(new Exception("Method " + this.method.getName() + "() should be void"));
        }
    }

    @Override
    protected int getModifiers() {
        return this.method.getModifiers();
    }

    public Class<?> getReturnType() {
        return this.method.getReturnType();
    }

    @Override
    public Class<?> getType() {
        return this.getReturnType();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return this.method.getDeclaringClass();
    }

    public void validateNoTypeParametersOnArgs(List<Throwable> errors) {
        new NoGenericTypeParametersValidator(this.method).validate(errors);
    }

    @Override
    public boolean isShadowedBy(FrameworkMethod other) {
        if (!other.getName().equals(this.getName())) {
            return false;
        }
        if (other.getParameterTypes().length != this.getParameterTypes().length) {
            return false;
        }
        for (int i = 0; i < other.getParameterTypes().length; ++i) {
            if (other.getParameterTypes()[i].equals(this.getParameterTypes()[i])) continue;
            return false;
        }
        return true;
    }

    public boolean equals(Object obj) {
        if (!FrameworkMethod.class.isInstance(obj)) {
            return false;
        }
        return ((FrameworkMethod)obj).method.equals(this.method);
    }

    public int hashCode() {
        return this.method.hashCode();
    }

    @Deprecated
    public boolean producesType(Type type) {
        return this.getParameterTypes().length == 0 && type instanceof Class && ((Class)type).isAssignableFrom(this.method.getReturnType());
    }

    private Class<?>[] getParameterTypes() {
        return this.method.getParameterTypes();
    }

    @Override
    public Annotation[] getAnnotations() {
        return this.method.getAnnotations();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return this.method.getAnnotation(annotationType);
    }

    public String toString() {
        return this.method.toString();
    }
}

