/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.stdlib.util;

import org.mapleir.stdlib.util.JavaDesc;

public class JavaDescSpecifier {
    public final String ownerRegex;
    public final String nameRegex;
    public final String descRegex;
    public final JavaDesc.DescType descType;

    public JavaDescSpecifier(String ownerRegex, String nameRegex, String descRegex, JavaDesc.DescType descType) {
        this.ownerRegex = ownerRegex;
        this.nameRegex = nameRegex;
        this.descRegex = descRegex;
        this.descType = descType;
        if (descType == JavaDesc.DescType.CLASS) assert (nameRegex.isEmpty() && descRegex.isEmpty());
    }

    public String toString() {
        return "(" + (Object)((Object)this.descType) + ")" + this.ownerRegex + "#" + this.nameRegex + this.descRegex;
    }

    public boolean matches(JavaDesc desc) {
        if (this.descType == JavaDesc.DescType.CLASS) {
            return desc.owner.matches(this.ownerRegex);
        }
        return desc.owner.matches(this.ownerRegex) && desc.name.matches(this.nameRegex) && desc.desc.matches(this.descRegex) && (this.descType == null || desc.descType == this.descType);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        JavaDescSpecifier javaDesc = (JavaDescSpecifier)o;
        if (this.ownerRegex != null ? !this.ownerRegex.equals(javaDesc.ownerRegex) : javaDesc.ownerRegex != null) {
            return false;
        }
        if (this.nameRegex != null ? !this.nameRegex.equals(javaDesc.nameRegex) : javaDesc.nameRegex != null) {
            return false;
        }
        if (this.descRegex != null ? !this.descRegex.equals(javaDesc.descRegex) : javaDesc.descRegex != null) {
            return false;
        }
        return !(this.descType != null ? !this.descType.equals((Object)javaDesc.descType) : javaDesc.descType != null);
    }

    public int hashCode() {
        int result = this.ownerRegex != null ? this.ownerRegex.hashCode() : 0;
        result = 31 * result + (this.nameRegex != null ? this.nameRegex.hashCode() : 0);
        result = 31 * result + (this.descRegex != null ? this.descRegex.hashCode() : 0);
        result = 31 * result + (this.descType != null ? this.descType.hashCode() : 0);
        return result;
    }
}

