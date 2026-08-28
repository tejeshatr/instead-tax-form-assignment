package com.taxform.formspec.model.annotation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taxform.formspec.FormSpecException;

/**
 * A presentation-only annotation bound to a field: text added inside the
 * field's box, hugging one of its sides (top, bottom, left, right) and
 * aligned along that edge — helper text like "Add your name here" or review
 * marks like "REVIEW". No data references, no absolute geometry: the
 * renderer resolves the field's rect and places the text relative to it.
 */
public record Annotation(
        @JsonProperty("field_ref") String fieldRef,
        Positioning positioning,
        Alignment alignment,
        String text,
        Double offset) {

    public Annotation {
        if (offset == null) {
            offset = 0.0;
        }
        if (positioning.isVerticalSide() && !alignment.isVertical()) {
            throw new FormSpecException("Positioning '" + positioning.jsonValue()
                    + "' takes a vertical alignment (top|middle|bottom), got: " + alignment.jsonValue());
        }
        if (!positioning.isVerticalSide() && alignment.isVertical()) {
            throw new FormSpecException("Positioning '" + positioning.jsonValue()
                    + "' takes a horizontal alignment (left|center|right), got: " + alignment.jsonValue());
        }
    }
}
