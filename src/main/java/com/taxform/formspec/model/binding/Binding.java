package com.taxform.formspec.model.binding;

import com.taxform.formspec.model.form.FormRef;

import java.util.List;
import java.util.Optional;

/**
 * The binding document — the fill instruction: which data values go into
 * which fields of a form template, how they are formatted, optional relative
 * placement for a particular consumer, true annotations (notes, marks,
 * highlights) per entry, and document-level stamps. Contains no absolute
 * field geometry.
 */
public record Binding(
        String kind,
        String specVersion,
        FormRef form,
        Optional<DataSource> dataSource,
        List<BindingEntry> fields,
        List<Stamp> stamps) {

    public Binding {
        dataSource = dataSource == null ? Optional.empty() : dataSource;
        fields = fields == null ? List.of() : List.copyOf(fields);
        stamps = stamps == null ? List.of() : List.copyOf(stamps);
    }
}
