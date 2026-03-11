/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.util;

import java.util.Objects;
import org.mapleir.stdlib.util.IUsesJavaDesc;
import org.mapleir.stdlib.util.JavaDesc;

public class JavaDescUse {
    public final JavaDesc target;
    public final IUsesJavaDesc flowElement;
    public final UseType flowType;

    public JavaDescUse(JavaDesc target, IUsesJavaDesc flowElement, UseType flowType) {
        this.target = target;
        this.flowElement = flowElement;
        this.flowType = flowType;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        JavaDescUse that = (JavaDescUse)o;
        return Objects.equals(this.target, that.target) && Objects.equals(this.flowElement, that.flowElement) && this.flowType == that.flowType;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.target, this.flowElement, this.flowType});
    }

    public String toString() {
        return this.target + " " + (Object)((Object)this.flowType) + " " + this.flowElement;
    }

    public static enum UseType {
        READ,
        WRITE,
        CALL;

    }
}

