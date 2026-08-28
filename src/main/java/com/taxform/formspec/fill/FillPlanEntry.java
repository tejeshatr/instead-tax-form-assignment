package com.taxform.formspec.fill;

import com.taxform.formspec.model.field.Rect;

import java.util.Optional;

/**
 * One row of a fill plan: an annotation entry and what happened to it.
 * {@code dx}/{@code dy} carry the entry's relative offset from the field's
 * rect, so a renderer can compute the exact print position.
 */
public record FillPlanEntry(
        String fieldRef,
        String fieldName,
        int pageIndex,
        Rect rect,
        double dx,
        double dy,
        Optional<String> formatted,
        FillStatus status,
        Optional<String> note) {

    public FillPlanEntry {
        formatted = formatted == null ? Optional.empty() : formatted;
        note = note == null ? Optional.empty() : note;
    }

    /** True when the entry carries a non-zero relative offset. */
    public boolean hasOffset() {
        return dx != 0 || dy != 0;
    }
}
