package com.taxform.formspec.model.field;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** What happens when a formatted value does not fit inside its field rect. */
public enum OverflowPolicy {
    /** Clip the value so it stays inside the box. */
    TRUNCATE("truncate"),
    /** Fail the fill with an error instead of printing an incomplete value. */
    ERROR("error");

    private final String json;

    OverflowPolicy(String json) {
        this.json = json;
    }

    @JsonValue
    public String jsonValue() {
        return json;
    }

    @JsonCreator
    public static OverflowPolicy from(String value) {
        for (OverflowPolicy o : values()) {
            if (o.json.equals(value)) {
                return o;
            }
        }
        throw new IllegalArgumentException("Unknown overflow: " + value);
    }
}
