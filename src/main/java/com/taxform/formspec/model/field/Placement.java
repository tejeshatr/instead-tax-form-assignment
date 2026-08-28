package com.taxform.formspec.model.field;

/**
 * Rules a renderer uses to place and fit a value inside the field's rect.
 * A conforming renderer must compute the exact text position from these
 * rules — annotations never carry raw coordinates.
 */
public record Placement(
        HAlign hAlign,
        VAlign vAlign,
        FontFamily fontFamily,
        FontSize fontSize,
        OverflowPolicy overflow,
        Boolean multiLine) {

    public Placement {
        if (fontFamily == null) {
            fontFamily = FontFamily.HELVETICA;
        }
        if (multiLine == null) {
            multiLine = false;
        }
    }

    public Placement(HAlign hAlign, VAlign vAlign, FontSize fontSize, OverflowPolicy overflow) {
        this(hAlign, vAlign, FontFamily.HELVETICA, fontSize, overflow, false);
    }
}
