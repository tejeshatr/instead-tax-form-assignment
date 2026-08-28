package com.taxform.formspec.model.form;

/** Reference to a form template from an annotation: form id, tax year, and template version. */
public record FormRef(String id, int taxYear, String version) {
}
