/*
 * Decompiled with CFR 0.152.
 */
package org.junit.runners.model;

import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.runners.model.Annotatable;

/*
 * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
 */
public abstract class FrameworkMember<T extends FrameworkMember<T>>
implements Annotatable {
    abstract boolean isShadowedBy(T var1);

    boolean isShadowedBy(List<T> members) {
        for (FrameworkMember each : members) {
            if (!this.isShadowedBy(each)) continue;
            return true;
        }
        return false;
    }

    protected abstract int getModifiers();

    public boolean isStatic() {
        return Modifier.isStatic(this.getModifiers());
    }

    public boolean isPublic() {
        return Modifier.isPublic(this.getModifiers());
    }

    public abstract String getName();

    public abstract Class<?> getType();

    public abstract Class<?> getDeclaringClass();
}

