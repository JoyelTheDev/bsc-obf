/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr.builtin;

import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.MapAttrs;
import org.mapleir.dot4j.attr.builtin.EndLabel;
import org.mapleir.dot4j.attr.builtin.Label;

public class ComplexLabel
extends Label
implements Attrs {
    private final boolean external;
    private final boolean floating;
    private final boolean decorated;
    private final Justification just;
    private final Location loc;

    private ComplexLabel(String value, boolean html, boolean external, boolean floating, boolean decorated, Justification just, Location loc) {
        super(value, html);
        this.external = external;
        this.floating = floating;
        this.decorated = decorated;
        this.just = just;
        this.loc = loc;
    }

    public EndLabel head() {
        return EndLabel.head(this, null, null);
    }

    public EndLabel head(double angle, double distance) {
        return EndLabel.head(this, angle, distance);
    }

    public EndLabel tail() {
        return EndLabel.tail(this, null, null);
    }

    public EndLabel tail(double angle, double distance) {
        return EndLabel.tail(this, angle, distance);
    }

    public ComplexLabel external() {
        return new ComplexLabel(this.value, this.html, true, this.floating, this.decorated, this.just, this.loc);
    }

    public ComplexLabel floating() {
        return new ComplexLabel(this.value, this.html, this.external, true, this.decorated, this.just, this.loc);
    }

    public ComplexLabel decorated() {
        return new ComplexLabel(this.value, this.html, this.external, this.floating, true, this.just, this.loc);
    }

    public ComplexLabel justify(Justification just) {
        return new ComplexLabel(this.value, this.html, this.external, this.floating, this.decorated, just, this.loc);
    }

    public ComplexLabel locate(Location loc) {
        return new ComplexLabel(this.value, this.html, this.external, this.floating, this.decorated, this.just, loc);
    }

    public boolean isExternal() {
        return this.external;
    }

    @Override
    public Attrs applyTo(MapAttrs mapAttrs) {
        mapAttrs.put(this.external ? "xlabel" : "label", this);
        if (this.floating) {
            mapAttrs.put("labelfloat", true);
        }
        if (this.decorated) {
            mapAttrs.put("decorate", true);
        }
        if (this.just == Justification.LEFT) {
            mapAttrs.put("labeljust", "l");
        }
        if (this.just == Justification.RIGHT) {
            mapAttrs.put("labeljust", "r");
        }
        if (this.loc == Location.TOP) {
            mapAttrs.put("labelloc", "t");
        }
        if (this.loc == Location.BOTTOM) {
            mapAttrs.put("labelloc", "b");
        }
        return mapAttrs;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Label label = (Label)o;
        return !(this.value == null ? label.value != null : !this.value.equals(label.value));
    }

    public int hashCode() {
        return this.value != null ? this.value.hashCode() : 0;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static ComplexLabel of(String value) {
        return new ComplexLabel(value, false, false, false, false, null, null);
    }

    public static ComplexLabel html(String value) {
        return new ComplexLabel(value, true, false, false, false, null, null);
    }

    public static enum Justification {
        LEFT,
        MIDDLE,
        RIGHT;

    }

    public static enum Location {
        TOP,
        CENTER,
        BOTTOM;

    }
}

