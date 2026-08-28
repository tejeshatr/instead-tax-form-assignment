package com.taxform.formspec.model.field;

import java.util.Optional;

/**
 * A named box on the form: its geometry ({@link Rect}), page, and placement
 * rules. Field ids are dotted and lowercase, inspired by IRS e-file naming
 * (e.g. {@code tp.ssn}, {@code l1a.wagesAmt}). Annotations reference fields
 * only by id — never by coordinates.
 */
public record Field(
        String id,
        String name,
        Optional<String> description,
        Optional<String> acroField,
        Rect rect,
        Placement placement) {

    public Field {
        description = description == null ? Optional.empty() : description;
        acroField = acroField == null ? Optional.empty() : acroField;
    }
}
