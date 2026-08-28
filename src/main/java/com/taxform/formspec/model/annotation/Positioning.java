package com.taxform.formspec.model.annotation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Which edge of a field's box an annotation hugs. */
public enum Positioning {
    LEFT("left"),
    RIGHT("right"),
    TOP("top"),
    BOTTOM("bottom");

    private final String json;

    Positioning(String json) {
        this.json = json;
    }

    @JsonValue
    public String jsonValue() {
        return json;
    }

    @JsonCreator
    public static Positioning from(String value) {
        for (Positioning p : values()) {
            if (p.json.equals(value)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown positioning: " + value);
    }

    /** True for left/right sides, which align vertically. */
    public boolean isVerticalSide() {
        return this == LEFT || this == RIGHT;
    }
}
