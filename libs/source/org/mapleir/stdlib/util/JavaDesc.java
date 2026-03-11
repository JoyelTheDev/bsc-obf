/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.util;

public class JavaDesc {
    public final String owner;
    public final String name;
    public final String desc;
    public final DescType descType;

    public JavaDesc(String owner, String name, String desc, DescType descType) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.descType = descType;
        if (descType == DescType.CLASS) assert (name.isEmpty() && desc.isEmpty());
    }

    public String toString() {
        return "(" + (Object)((Object)this.descType) + ")" + this.owner + "#" + this.name + this.desc;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        JavaDesc javaDesc = (JavaDesc)o;
        if (this.owner != null ? !this.owner.equals(javaDesc.owner) : javaDesc.owner != null) {
            return false;
        }
        if (this.name != null ? !this.name.equals(javaDesc.name) : javaDesc.name != null) {
            return false;
        }
        if (this.desc != null ? !this.desc.equals(javaDesc.desc) : javaDesc.desc != null) {
            return false;
        }
        return !(this.descType != null ? !this.descType.equals((Object)javaDesc.descType) : javaDesc.descType != null);
    }

    public int hashCode() {
        int result = this.owner != null ? this.owner.hashCode() : 0;
        result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
        result = 31 * result + (this.desc != null ? this.desc.hashCode() : 0);
        result = 31 * result + (this.descType != null ? this.descType.hashCode() : 0);
        return result;
    }

    public static enum DescType {
        FIELD,
        METHOD,
        CLASS;

    }
}

