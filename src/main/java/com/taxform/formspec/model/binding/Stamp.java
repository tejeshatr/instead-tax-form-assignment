package com.taxform.formspec.model.binding;

import com.taxform.formspec.model.field.Placement;
import com.taxform.formspec.model.field.Rect;

/**
 * Page-level overlay text — "DRAFT", "AMENDED", "CLIENT COPY — DO NOT FILE" —
 * drawn in its own rect on a page. Document-level (stamps span a page, not a
 * single field).
 */
public record Stamp(String text, int page, Rect rect, Placement placement) {
}
