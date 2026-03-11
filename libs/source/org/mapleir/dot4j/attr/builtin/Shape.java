/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.attr.builtin;

import org.mapleir.dot4j.attr.Attr;
import org.mapleir.dot4j.attr.Attrs;

public class Shape
extends Attr<String> {
    private static final String SHAPE = "shape";
    public static final Shape BOX = new Shape("box");
    public static final Shape ELLIPSE = new Shape("ellipse");
    public static final Shape CIRCLE = new Shape("circle");
    public static final Shape POINT = new Shape("point");
    public static final Shape EGG = new Shape("egg");
    public static final Shape TRIANGLE = new Shape("triangle");
    public static final Shape DIAMOND = new Shape("diamond");
    public static final Shape TRAPEZIUM = new Shape("trapezium");
    public static final Shape PARALLELOGRAM = new Shape("parallelogram");
    public static final Shape HOUSE = new Shape("house");
    public static final Shape PENTAGON = new Shape("pentagon");
    public static final Shape HEXAGON = new Shape("hexagon");
    public static final Shape SEPTAGON = new Shape("septagon");
    public static final Shape OCTAGON = new Shape("octagon");
    public static final Shape DOUBLE_CIRCLE = new Shape("doublecircle");
    public static final Shape DOUBLE_OCTAGON = new Shape("doubleoctagon");
    public static final Shape TRIPLE_OCTAGON = new Shape("tripleoctagon");
    public static final Shape INV_TRIANGLE = new Shape("invtriangle");
    public static final Shape INV_TRAPEZIUM = new Shape("invtrapezium");
    public static final Shape INV_HOUSE = new Shape("invhouse");
    public static final Shape RECTANGLE = new Shape("rectangle");
    public static final Shape NONE = new Shape("none");

    private Shape(String value) {
        super(SHAPE, value);
    }

    public static Attrs mDiamond(String topLabel, String bottomLabel) {
        return Attrs.attrs(Attrs.attr(SHAPE, "Mdiamond"), Attrs.attr("toplabel", topLabel), Attrs.attr("bottomlabel", bottomLabel));
    }

    public static Attrs mSquare(String topLabel, String bottomLabel) {
        return Attrs.attrs(Attrs.attr(SHAPE, "Msquare"), Attrs.attr("toplabel", topLabel), Attrs.attr("bottomlabel", bottomLabel));
    }

    public static Attrs mCircle(String topLabel, String bottomLabel) {
        return Attrs.attrs(Attrs.attr(SHAPE, "Mcircle"), Attrs.attr("toplabel", topLabel), Attrs.attr("bottomlabel", bottomLabel));
    }

    public static Attrs polygon(int sides, double skew, double distortion) {
        return Attrs.attrs(Attrs.attr(SHAPE, "polygon"), Attrs.attr("sides", sides), Attrs.attr("skew", skew), Attrs.attr("distortion", distortion));
    }
}

