package com.taxform.formspec.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Font sizing strategy.
 * <ul>
 *   <li>{@link #FIXED} — always render at {@code size} points.</li>
 *   <li>{@link #AUTOFIT} — shrink to fit the rect, never below {@code min}
 *       and never above {@code max} points.</li>
 * </ul>
 */
public enum FontSizeMode {
    FIXED("fixed"),
    AUTOFIT("autofit");

    private final String json;

    FontSizeMode(String json) {
        this.json = json;
    }

    @JsonValue
    public String jsonValue() {
        return json;
    }

    @JsonCreator
    public static FontSizeMode from(String value) {
        for (FontSizeMode m : values()) {
            if (m.json.equals(value)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Unknown fontSize mode: " + value);
    }
}
