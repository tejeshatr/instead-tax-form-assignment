package com.taxform.formspec.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Vertical alignment of a value inside its field rect. */
public enum VAlign {
    TOP("top"),
    MIDDLE("middle"),
    BOTTOM("bottom");

    private final String json;

    VAlign(String json) {
        this.json = json;
    }

    @JsonValue
    public String jsonValue() {
        return json;
    }

    @JsonCreator
    public static VAlign from(String value) {
        for (VAlign v : values()) {
            if (v.json.equals(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown vAlign: " + value);
    }
}
