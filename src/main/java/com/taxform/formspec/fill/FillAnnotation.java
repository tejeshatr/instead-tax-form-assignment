package com.taxform.formspec.fill;

import com.taxform.formspec.model.binding.Mark;

import java.util.Optional;

/**
 * A true annotation collected from a binding entry during the fill: a human
 * note, a printed mark, or a reviewer-attention highlight attached to a
 * field. Carried on the {@link FillPlan} so consumers can surface them.
 */
public record FillAnnotation(
        String fieldRef,
        Optional<String> note,
        Optional<Mark> mark,
        boolean highlight) {

    public FillAnnotation {
        note = note == null ? Optional.empty() : note;
        mark = mark == null ? Optional.empty() : mark;
    }
}
