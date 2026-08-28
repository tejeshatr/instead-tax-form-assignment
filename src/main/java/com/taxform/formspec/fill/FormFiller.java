package com.taxform.formspec.fill;

import com.taxform.formspec.FormSpecException;
import com.taxform.formspec.format.Formatter;
import com.taxform.formspec.format.FormatterRegistry;
import com.taxform.formspec.model.annotation.AnnotationDocument;
import com.taxform.formspec.model.binding.Binding;
import com.taxform.formspec.model.binding.BindingEntry;
import com.taxform.formspec.model.field.Field;
import com.taxform.formspec.model.form.FormTemplate;
import com.taxform.formspec.model.page.FormPage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fills a form: resolves every binding entry against the data set, formats
 * each value, and returns a {@link FillPlan} — the resolved value and
 * relative offset for every box, plus the binding's annotations (notes,
 * marks, highlights, stamps). A renderer then draws the plan per each
 * field's placement rules (with the entry's placement override merged on
 * top); the dry-run demo prints it instead. This class is the core of the
 * pipeline and is renderer-independent.
 */
public final class FormFiller {

    private FormFiller() {
    }

    public static FillPlan fill(FormTemplate template, Binding binding, String dataJson) {
        return fill(template, binding, null, dataJson);
    }

    /** Fills a form with an optional annotation document layered on top. */
    public static FillPlan fill(FormTemplate template, Binding binding, AnnotationDocument annotationDocument, String dataJson) {
        Map<String, Field> fieldById = new HashMap<>();
        Map<String, Integer> pageByField = new HashMap<>();
        for (FormPage page : template.pages()) {
            for (Field field : page.fields()) {
                fieldById.put(field.id(), field);
                pageByField.put(field.id(), page.index());
            }
        }

        List<FillPlanEntry> entries = new ArrayList<>();
        List<FillAnnotation> annotations = new ArrayList<>();
        for (BindingEntry entry : binding.fields()) {
            Field field = fieldById.get(entry.fieldRef());
            if (field == null) {
                // Should be unreachable: parseBinding validates fieldRefs.
                entries.add(new FillPlanEntry(entry.fieldRef(), entry.fieldRef(), 0, null,
                        0, 0, Optional.empty(), FillStatus.ERROR_PATH,
                        Optional.of("unknown fieldRef")));
                continue;
            }

            double dx = entry.placement().map(p -> p.dx()).orElse(0.0);
            double dy = entry.placement().map(p -> p.dy()).orElse(0.0);

            entry.annotation().ifPresent(a ->
                    annotations.add(new FillAnnotation(entry.fieldRef(), a.note(), a.mark(), a.highlight())));

            Optional<Object> raw;
            try {
                raw = JsonPathResolver.resolve(dataJson, entry.jsonPath());
            } catch (FormSpecException e) {
                entries.add(fillPlanEntry(entry, field, pageByField, dx, dy, Optional.empty(),
                        FillStatus.ERROR_PATH, e.getMessage()));
                continue;
            }

            if (raw.isEmpty() || raw.get() == null) {
                String note = raw.isEmpty() ? "no value at path" : "value is JSON null";
                if (entry.required()) {
                    entries.add(fillPlanEntry(entry, field, pageByField, dx, dy, Optional.empty(),
                            FillStatus.ERROR_MISSING_REQUIRED, note));
                } else {
                    entries.add(fillPlanEntry(entry, field, pageByField, dx, dy, Optional.empty(),
                            FillStatus.SKIPPED_MISSING, note));
                }
                continue;
            }

            String formatted;
            try {
                formatted = format(entry, raw.get());
            } catch (FormSpecException e) {
                entries.add(fillPlanEntry(entry, field, pageByField, dx, dy, Optional.empty(),
                        FillStatus.ERROR_FORMAT, e.getMessage()));
                continue;
            }

            if (formatted == null) {
                // Checkbox contract: value does not match checkedValue → leave blank.
                entries.add(fillPlanEntry(entry, field, pageByField, dx, dy, Optional.empty(),
                        FillStatus.SKIPPED_MISSING, "checkbox not checked"));
                continue;
            }
            if (formatted.isBlank()) {
                String note = "value is blank";
                if (entry.required()) {
                    entries.add(fillPlanEntry(entry, field, pageByField, dx, dy, Optional.empty(),
                            FillStatus.ERROR_MISSING_REQUIRED, note));
                } else {
                    entries.add(fillPlanEntry(entry, field, pageByField, dx, dy, Optional.empty(),
                            FillStatus.SKIPPED_MISSING, note));
                }
                continue;
            }
            entries.add(fillPlanEntry(entry, field, pageByField, dx, dy, Optional.of(formatted),
                    FillStatus.FILLED, null));
        }
        return new FillPlan(entries, annotations, binding.stamps(),
                annotationDocument == null ? List.of() : annotationDocument.annotations());
    }

    /** Applies the entry's formatter, or plain stringification when none is given. */
    private static String format(BindingEntry entry, Object raw) {
        if (entry.formatter().isPresent()) {
            var format = entry.formatter().get();
            Formatter formatter = FormatterRegistry.get(format.type())
                    .orElseThrow(() -> new FormSpecException("Unknown formatter type: " + format.type()));
            return formatter.format(raw, format.params());
        }
        return String.valueOf(raw);
    }

    private static FillPlanEntry fillPlanEntry(
            BindingEntry entry,
            Field field,
            Map<String, Integer> pageByField,
            double dx,
            double dy,
            Optional<String> formatted,
            FillStatus status,
            String note) {
        return new FillPlanEntry(entry.fieldRef(), field.name(), pageByField.get(entry.fieldRef()),
                field.rect(), dx, dy, formatted, status,
                note == null ? Optional.empty() : Optional.of(note));
    }
}
