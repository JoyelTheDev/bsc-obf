/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j.model;

import java.util.Optional;
import java.util.stream.Stream;

public enum Compass {
    NORTH,
    NORTH_EAST,
    EAST,
    SOUTH_EAST,
    SOUTH,
    SOUTH_WEST,
    WEST,
    NORTH_WEST,
    CENTER;

    final String value = Compass.getDotName(this.name());

    private static String getDotName(String name) {
        String[] words;
        StringBuilder sb = new StringBuilder();
        for (String word : words = name.split("_")) {
            sb.append(Character.toLowerCase(word.charAt(0)));
        }
        return sb.toString();
    }

    public static Optional<Compass> of(String value) {
        return Stream.of(Compass.values()).filter(c -> c.value.equals(value)).findFirst();
    }
}

