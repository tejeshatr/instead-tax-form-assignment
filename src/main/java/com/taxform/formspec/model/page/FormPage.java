package com.taxform.formspec.model.page;

import com.taxform.formspec.model.field.Field;

import java.util.List;
import java.util.Optional;

/** One page of the form: its 0-based index and the fields that live on it. */
public record FormPage(int index, Optional<String> label, List<Field> fields) {

    public FormPage {
        label = label == null ? Optional.empty() : label;
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
