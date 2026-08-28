package com.taxform.formspec.model.form;

import java.util.Optional;

/** Identity of a form template: the form itself and this template version. */
public record FormMeta(
        String id,
        String name,
        int taxYear,
        String version,
        Optional<String> officialAcroForm) {

    public FormMeta {
        officialAcroForm = officialAcroForm == null ? Optional.empty() : officialAcroForm;
    }
}
