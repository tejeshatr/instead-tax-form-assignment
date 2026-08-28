package com.taxform.formspec;

import com.taxform.formspec.model.field.FontSize;
import com.taxform.formspec.model.field.FontSizeMode;
import com.taxform.formspec.model.field.HAlign;
import com.taxform.formspec.model.field.OverflowPolicy;
import com.taxform.formspec.model.field.Placement;
import com.taxform.formspec.model.field.PlacementOverride;
import com.taxform.formspec.model.field.VAlign;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Proves relative-placement merge semantics: unspecified properties inherit. */
class PlacementOverrideTest {

    private static final Placement BASE = new Placement(
            HAlign.RIGHT, VAlign.MIDDLE, com.taxform.formspec.model.field.FontFamily.COURIER,
            new FontSize(FontSizeMode.FIXED, 9.0, null, null), OverflowPolicy.TRUNCATE, false);

    @Test
    void unspecifiedPropertiesInherit() {
        PlacementOverride override = new PlacementOverride(0.0, 0.0, HAlign.LEFT, null, null, null, null, null);
        Placement merged = override.apply(BASE);
        assertEquals(HAlign.LEFT, merged.hAlign());
        assertEquals(VAlign.MIDDLE, merged.vAlign());
        assertEquals(com.taxform.formspec.model.field.FontFamily.COURIER, merged.fontFamily());
        assertEquals(9.0, merged.fontSize().size());
        assertEquals(OverflowPolicy.TRUNCATE, merged.overflow());
        assertEquals(false, merged.multiLine());
    }

    @Test
    void nullOffsetsDefaultToZero() {
        PlacementOverride override = new PlacementOverride(null, null, null, null, null, null, null, null);
        assertEquals(0.0, override.dx());
        assertEquals(0.0, override.dy());
        assertEquals(BASE, override.apply(BASE));
    }

    @Test
    void fullOverrideReplacesEverything() {
        PlacementOverride override = new PlacementOverride(2.0, -3.0, HAlign.CENTER, VAlign.TOP,
                com.taxform.formspec.model.field.FontFamily.HELVETICA,
                new FontSize(FontSizeMode.AUTOFIT, null, 7.0, 10.0), OverflowPolicy.ERROR, true);
        Placement merged = override.apply(BASE);
        assertEquals(HAlign.CENTER, merged.hAlign());
        assertEquals(VAlign.TOP, merged.vAlign());
        assertEquals(com.taxform.formspec.model.field.FontFamily.HELVETICA, merged.fontFamily());
        assertEquals(FontSizeMode.AUTOFIT, merged.fontSize().mode());
        assertEquals(OverflowPolicy.ERROR, merged.overflow());
        assertEquals(true, merged.multiLine());
    }
}
