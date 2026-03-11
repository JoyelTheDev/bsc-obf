/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr.builtin;

import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.MapAttrs;
import org.mapleir.dot4j.attr.builtin.Label;

public class EndLabel
extends Label
implements Attrs {
    private final String key;
    private final Double angle;
    private final Double distance;

    private EndLabel(String key, String value, boolean html, Double angle, Double distance) {
        super(value, html);
        this.key = key;
        this.angle = angle;
        this.distance = distance;
    }

    public static EndLabel head(Label label, Double angle, Double distance) {
        return new EndLabel("headlabel", label.value, label.html, angle, distance);
    }

    public static EndLabel tail(Label label, Double angle, Double distance) {
        return new EndLabel("taillabel", label.value, label.html, angle, distance);
    }

    @Override
    public Attrs applyTo(MapAttrs mapAttrs) {
        mapAttrs.put(this.key, this);
        if (this.angle != null) {
            mapAttrs.put("labelangle", this.angle);
        }
        if (this.distance != null) {
            mapAttrs.put("labeldistance", this.distance);
        }
        return mapAttrs;
    }
}

