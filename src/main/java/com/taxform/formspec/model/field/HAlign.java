package com.taxform.formspec.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Horizontal alignment of a value inside its field rect. */
public enum HAlign {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right");

    private final String json;

    HAlign(String json) {
        this.json = json;
    }

    @JsonValue
    public String jsonValue() {
        return json;
    }

    @JsonCreator
    public static HAlign from(String value) {
        for (HAlign h : values()) {
            if (h.json.equals(value)) {
                return h;
            }
        }
        throw new IllegalArgumentException("Unknown hAlign: " + value);
    }
}
