package com.taxform.formspec.model.annotation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Where an annotation sits along its field's edge: vertical values
 * ({@code top}, {@code middle}, {@code bottom}) for left/right sides,
 * horizontal values ({@code left}, {@code center}, {@code right}) for
 * top/bottom sides.
 */
public enum Alignment {
    TOP("top", true),
    MIDDLE("middle", true),
    BOTTOM("bottom", true),
    LEFT("left", false),
    CENTER("center", false),
    RIGHT("right", false);

    private final String json;
    private final boolean vertical;

    Alignment(String json, boolean vertical) {
        this.json = json;
        this.vertical = vertical;
    }

    @JsonValue
    public String jsonValue() {
        return json;
    }

    @JsonCreator
    public static Alignment from(String value) {
        for (Alignment a : values()) {
            if (a.json.equals(value)) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unknown alignment: " + value);
    }

    /** True for top/middle/bottom — valid along left/right sides. */
    public boolean isVertical() {
        return vertical;
    }
}
