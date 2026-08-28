package com.taxform.formspec.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Font family for rendered values. All three are PDF-standard fonts
 * (Helvetica, Courier, Times), so any conforming renderer can produce them
 * without embedding assets. Default: {@link #HELVETICA}.
 */
public enum FontFamily {
    HELVETICA("helvetica"),
    COURIER("courier"),
    TIMES("times");

    private final String json;

    FontFamily(String json) {
        this.json = json;
    }

    @JsonValue
    public String jsonValue() {
        return json;
    }

    @JsonCreator
    public static FontFamily from(String value) {
        for (FontFamily f : values()) {
            if (f.json.equals(value)) {
                return f;
            }
        }
        throw new IllegalArgumentException("Unknown fontFamily: " + value);
    }
}
