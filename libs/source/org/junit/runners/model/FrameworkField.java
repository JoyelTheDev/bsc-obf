/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import org.junit.runners.model.FrameworkMember;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public class FrameworkField
extends FrameworkMember<FrameworkField> {
    private final Field field;

    FrameworkField(Field field) {
        if (field == null) {
            throw new NullPointerException("FrameworkField cannot be created without an underlying field.");
        }
        this.field = field;
    }

    @Override
    public String getName() {
        return this.getField().getName();
    }

    @Override
    public Annotation[] getAnnotations() {
        return this.field.getAnnotations();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return this.field.getAnnotation(annotationType);
    }

    @Override
    public boolean isShadowedBy(FrameworkField otherMember) {
        return otherMember.getName().equals(this.getName());
    }

    @Override
    protected int getModifiers() {
        return this.field.getModifiers();
    }

    public Field getField() {
        return this.field;
    }

    @Override
    public Class<?> getType() {
        return this.field.getType();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return this.field.getDeclaringClass();
    }

    public Object get(Object target) throws IllegalArgumentException, IllegalAccessException {
        return this.field.get(target);
    }

    public String toString() {
        return this.field.toString();
    }
}

