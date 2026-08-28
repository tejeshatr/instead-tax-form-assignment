package com.taxform.formspec.model.binding;

import com.taxform.formspec.model.field.PlacementOverride;

import java.util.Optional;

/**
 * A short printed text added on top of a field, e.g. "REVIEW" or
 * "see attachment". Positioned relative to the field's rect; without a
 * placement override it sits at the field's top-right corner (dx, dy = 0
 * by default — renderers may choose a sensible default offset).
 */
public record Mark(String text, Optional<PlacementOverride> placement) {

    public Mark {
        placement = placement == null ? Optional.empty() : placement;
    }
}
