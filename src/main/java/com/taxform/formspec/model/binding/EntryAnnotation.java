package com.taxform.formspec.model.binding;

import java.util.Optional;

/**
 * A true annotation — something extra added on top of a field, beyond the
 * data binding and its formatting:
 * <ul>
 *   <li>{@code note} — human comment attached to the field (tooling, not printed)</li>
 *   <li>{@code mark} — short text drawn near the field (printed)</li>
 *   <li>{@code highlight} — reviewer-attention flag (tooling; renderers may draw emphasis)</li>
 * </ul>
 */
public record EntryAnnotation(
        Optional<String> note,
        Optional<Mark> mark,
        Boolean highlight) {

    public EntryAnnotation {
        note = note == null ? Optional.empty() : note;
        mark = mark == null ? Optional.empty() : mark;
        if (highlight == null) {
            highlight = false;
        }
    }
}
