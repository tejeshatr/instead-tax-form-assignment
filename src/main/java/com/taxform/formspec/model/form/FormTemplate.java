package com.taxform.formspec.model.form;

import com.taxform.formspec.model.page.FormPage;
import com.taxform.formspec.model.page.PageSize;

import java.util.List;

/**
 * The geometry document: where every box is on the form and how values are
 * placed inside it. Written once per form and tax-year version; shared by
 * any number of annotations. Contains no data references.
 */
public record FormTemplate(
        String kind,
        String specVersion,
        FormMeta form,
        PageSize pageSize,
        List<FormPage> pages) {

    public FormTemplate {
        pages = pages == null ? List.of() : List.copyOf(pages);
    }
}
