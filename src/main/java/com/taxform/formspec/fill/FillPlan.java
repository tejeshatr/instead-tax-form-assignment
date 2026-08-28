package com.taxform.formspec.fill;

import com.taxform.formspec.model.annotation.Annotation;
import com.taxform.formspec.model.binding.Stamp;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The result of a fill: for every binding entry, the resolved value and the
 * box it would be printed into; the binding's embedded annotations (notes,
 * marks, highlights) and page-level stamps; and the annotation document's
 * presentation-only {@link Annotation}s. {@link #render()} produces the
 * human-readable table shown in the demo and the walkthrough video.
 */
public record FillPlan(
        List<FillPlanEntry> entries,
        List<FillAnnotation> bindingAnnotations,
        List<Stamp> stamps,
        List<Annotation> annotations) {

    public FillPlan {
        entries = List.copyOf(entries);
        bindingAnnotations = bindingAnnotations == null ? List.of() : List.copyOf(bindingAnnotations);
        stamps = stamps == null ? List.of() : List.copyOf(stamps);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }

    public FillPlan(List<FillPlanEntry> entries) {
        this(entries, List.of(), List.of(), List.of());
    }

    private static final String ROW = "%-4s  %-30s %-46s %-34s %-15s %-22s %s%n";

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(ROW, "Page", "Field", "Name", "Rect (x, y, w x h) + offset", "Value", "Status", "Note"));
        sb.append("-".repeat(180)).append('\n');
        for (FillPlanEntry e : entries) {
            String rect = e.rect() == null ? "" : e.rect() + (e.hasOffset() ? " Δ" + fmt(e.dx()) + "," + fmt(e.dy()) : "");
            sb.append(String.format(ROW,
                    e.pageIndex(),
                    truncate(e.fieldRef(), 30),
                    truncate(e.fieldName(), 46),
                    rect,
                    truncate(e.formatted().orElse(""), 15),
                    e.status(),
                    truncate(e.note().orElse(""), 60)));
        }
        sb.append("-".repeat(180)).append('\n');
        Map<FillStatus, Long> counts = new EnumMap<>(FillStatus.class);
        for (FillPlanEntry e : entries) {
            counts.merge(e.status(), 1L, Long::sum);
        }
        sb.append("Total: ").append(entries.size()).append(" entries");
        for (FillStatus s : FillStatus.values()) {
            long n = counts.getOrDefault(s, 0L);
            if (n > 0) {
                sb.append(" | ").append(s).append(": ").append(n);
            }
        }
        if (!annotations.isEmpty() || !bindingAnnotations.isEmpty() || !stamps.isEmpty()) {
            sb.append("\n\nAnnotations:\n");
            for (Annotation a : annotations) {
                sb.append(String.format("  %-10s %-30s [%s, %s]%s  \"%s\"%n",
                        "annotation", truncate(a.fieldRef(), 30),
                        a.positioning().jsonValue(), a.alignment().jsonValue(),
                        a.offset() != 0 ? " +" + fmt(a.offset()) : "",
                        a.text()));
            }
            for (FillAnnotation a : bindingAnnotations) {
                a.note().ifPresent(n -> sb.append(String.format("  %-10s %-30s %s%n",
                        "note", truncate(a.fieldRef(), 30), n)));
                a.mark().ifPresent(m -> sb.append(String.format("  %-10s %-30s \"%s\"%s%n",
                        "mark", truncate(a.fieldRef(), 30), m.text(),
                        m.placement().map(p -> " Δ" + fmt(p.dx()) + "," + fmt(p.dy())).orElse(""))));
                if (a.highlight()) {
                    sb.append(String.format("  %-10s %s%n", "highlight", truncate(a.fieldRef(), 30)));
                }
            }
            for (Stamp stamp : stamps) {
                sb.append(String.format("  %-10s page %d  \"%s\"  %s%n",
                        "stamp", stamp.page(), stamp.text(), stamp.rect()));
            }
        }
        return sb.toString();
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? Long.toString(Math.round(v)) : Double.toString(v);
    }

    private static String truncate(String s, int width) {
        if (s == null) {
            return "";
        }
        return s.length() <= width ? s : s.substring(0, Math.max(0, width - 1)) + "…";
    }
}
