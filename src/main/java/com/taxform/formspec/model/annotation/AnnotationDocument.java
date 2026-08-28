package com.taxform.formspec.model.annotation;

import com.taxform.formspec.model.form.FormRef;

import java.util.List;

/**
 * The annotation document: presentation-only text layered on top of a form,
 * each {@link Annotation} bound to a template field and positioned inside
 * its box. Authored separately from the binding (data wiring) and the form
 * template (geometry); consumed at render time.
 */
public record AnnotationDocument(
        String kind,
        String specVersion,
        FormRef form,
        List<Annotation> annotations) {

    public AnnotationDocument {
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }
}
