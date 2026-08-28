package com.taxform.formspec.model.field;

/**
 * Relative positioning declared by an annotation entry: a {@code dx}/{@code dy}
 * offset from the field's rect, plus optional overrides of individual
 * placement properties. Every property is optional — unspecified ones
 * inherit the field's placement from the form template via {@link #apply}.
 */
public record PlacementOverride(
        Double dx,
        Double dy,
        HAlign hAlign,
        VAlign vAlign,
        FontFamily fontFamily,
        FontSize fontSize,
        OverflowPolicy overflow,
        Boolean multiLine) {

    public PlacementOverride {
        if (dx == null) {
            dx = 0.0;
        }
        if (dy == null) {
            dy = 0.0;
        }
    }

    /** Merges this override over the field's base placement; unspecified properties inherit. */
    public Placement apply(Placement base) {
        return new Placement(
                hAlign != null ? hAlign : base.hAlign(),
                vAlign != null ? vAlign : base.vAlign(),
                fontFamily != null ? fontFamily : base.fontFamily(),
                fontSize != null ? fontSize : base.fontSize(),
                overflow != null ? overflow : base.overflow(),
                multiLine != null ? multiLine : base.multiLine());
    }
}
