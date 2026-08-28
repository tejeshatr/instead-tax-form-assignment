package com.taxform.formspec.model.field;

import com.taxform.formspec.FormSpecException;

/**
 * Font size parameters, flattened (no polymorphic deserialization):
 * <ul>
 *   <li>{@link FontSizeMode#FIXED} requires {@code size}.</li>
 *   <li>{@link FontSizeMode#AUTOFIT} requires {@code min} and {@code max}.</li>
 * </ul>
 * All values are in points.
 */
public record FontSize(FontSizeMode mode, Double size, Double min, Double max) {

    public FontSize {
        if (mode == FontSizeMode.FIXED) {
            if (size == null || size <= 0) {
                throw new FormSpecException("fontSize mode 'fixed' requires a positive 'size'");
            }
        } else if (mode == FontSizeMode.AUTOFIT) {
            if (min == null || max == null || min <= 0 || max <= 0) {
                throw new FormSpecException("fontSize mode 'autofit' requires positive 'min' and 'max'");
            }
            if (min > max) {
                throw new FormSpecException("fontSize 'min' must not exceed 'max'");
            }
        }
    }

    /** The font size a fixed-mode field is rendered at. */
    public double fixedSize() {
        return size;
    }
}
