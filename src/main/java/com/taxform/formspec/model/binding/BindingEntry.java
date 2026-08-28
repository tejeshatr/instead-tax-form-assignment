package com.taxform.formspec.model.binding;

import com.taxform.formspec.format.Format;
import com.taxform.formspec.model.field.PlacementOverride;

import java.util.Optional;

/**
 * One entry of a binding: binds a template field id to a value in the data
 * set. The JSONPath subset is defined in docs/spec.md and must resolve to
 * exactly one scalar (or one boolean for checkboxes). Optional {@code
 * placement} positions the value relative to the field's box; optional
 * {@code annotation} adds notes/marks/highlights on top of the field.
 */
public record BindingEntry(
        String fieldRef,
        String jsonPath,
        Optional<Format> formatter,
        Boolean required,
        Optional<PlacementOverride> placement,
        Optional<EntryAnnotation> annotation,
        Optional<String> description) {

    public BindingEntry {
        formatter = formatter == null ? Optional.empty() : formatter;
        if (required == null) {
            required = false;
        }
        placement = placement == null ? Optional.empty() : placement;
        annotation = annotation == null ? Optional.empty() : annotation;
        description = description == null ? Optional.empty() : description;
    }
}
