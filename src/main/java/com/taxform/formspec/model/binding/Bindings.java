package com.taxform.formspec.model.binding;

import com.taxform.formspec.FormSpecException;
import com.taxform.formspec.SpecVersion;
import com.taxform.formspec.format.Format;
import com.taxform.formspec.model.field.Placement;
import com.taxform.formspec.model.field.PlacementOverride;
import com.taxform.formspec.model.field.Rect;
import com.taxform.formspec.model.form.FormRef;
import com.taxform.formspec.model.form.FormTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Fluent builder for authoring a {@link Binding} — the fill-instruction
 * document — from Java code instead of hand-writing JSON. Users select the
 * template's field ids and bind each to a data path; entries may also carry
 * relative positioning ({@link FieldBinding#at},
 * {@link FieldBinding#withPlacement}) and true annotations ({@link
 * FieldBinding#note}, {@link FieldBinding#marked}, {@link FieldBinding#highlighted});
 * {@link #stamp} adds page-level stamps.
 *
 * <pre>{@code
 * Binding binding = Bindings.forForm("f1040", 2025, "2025-1")
 *         .field("l1a.wagesAmt").from("$.taxpayer.w2s[0].box1Wages").asCurrency().required()
 *         .field("l7a.capitalGainAmt").from("$.income.capitalGain").asCurrency()
 *             .marked("REVIEW").highlighted()
 *         .field("sig.signature").from("$.signature.name").at(-2, -1)
 *         .stamp("DRAFT — DO NOT FILE", 0, new Rect(450, 30, 100, 14),
 *             new Placement(HAlign.CENTER, VAlign.MIDDLE, new FontSize(FontSizeMode.FIXED, 10.0, null, null), OverflowPolicy.TRUNCATE))
 *         .build(template); // optional: cross-checks every fieldRef against the template
 * }</pre>
 */
public final class Bindings {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private final String formId;
    private final int taxYear;
    private final String templateVersion;
    private DataSource dataSource;
    private final List<BindingEntry> entries = new ArrayList<>();
    private final List<Stamp> stamps = new ArrayList<>();

    private Bindings(String formId, int taxYear, String templateVersion) {
        this.formId = formId;
        this.taxYear = taxYear;
        this.templateVersion = templateVersion;
    }

    /** Starts a binding against the form template with the given identity. */
    public static Bindings forForm(String formId, int taxYear, String templateVersion) {
        return new Bindings(formId, taxYear, templateVersion);
    }

    /** Declares the data source the binding's paths are written against (optional). */
    public Bindings fromDataSource(String name, String dataFormat, String version) {
        this.dataSource = new DataSource(name, dataFormat, Optional.ofNullable(version));
        return this;
    }

    /** Begins a new field binding for the given template field id. */
    public FieldBinding field(String fieldRef) {
        return new FieldBinding(this, fieldRef);
    }

    /** Adds a page-level stamp, e.g. "DRAFT", drawn in its own rect. */
    public Bindings stamp(String text, int page, Rect rect, Placement placement) {
        stamps.add(new Stamp(text, page, rect, placement));
        return this;
    }

    public Binding build() {
        return new Binding("binding", SpecVersion.CURRENT,
                new FormRef(formId, taxYear, templateVersion),
                Optional.ofNullable(dataSource), List.copyOf(entries), List.copyOf(stamps));
    }

    /** Builds the binding and cross-checks every fieldRef against the template. */
    public Binding build(FormTemplate template) {
        Set<String> fieldIds = new HashSet<>();
        for (var page : template.pages()) {
            for (var field : page.fields()) {
                fieldIds.add(field.id());
            }
        }
        for (var entry : entries) {
            if (!fieldIds.contains(entry.fieldRef())) {
                throw new FormSpecException("fieldRef '" + entry.fieldRef()
                        + "' does not exist in template " + template.form().id()
                        + " " + template.form().version());
            }
        }
        return build();
    }

    /** Serializes the built binding to spec JSON (pretty-printed). */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(build());
        } catch (JacksonException e) {
            throw new FormSpecException("Could not serialize binding", e);
        }
    }

    void add(BindingEntry entry) {
        for (var existing : entries) {
            if (existing.fieldRef().equals(entry.fieldRef())) {
                throw new FormSpecException("Duplicate fieldRef in binding: " + entry.fieldRef());
            }
        }
        entries.add(entry);
    }

    /**
     * Fluent configuration of a single binding entry. Terminal methods return
     * this binding so calls chain; {@link #field(String)} starts the next
     * entry; {@link #build()}, {@link #build(FormTemplate)}, and
     * {@link #toJson()} finish the binding.
     */
    public static final class FieldBinding {

        private final Bindings binding;
        private final String fieldRef;
        private String jsonPath;
        private Format format;
        private boolean required;
        private String description;
        private Double dx;
        private Double dy;
        private PlacementOverride overrides;
        private String note;
        private String markText;
        private Double markDx;
        private Double markDy;
        private boolean highlighted;
        private boolean added;

        private FieldBinding(Bindings binding, String fieldRef) {
            this.binding = binding;
            this.fieldRef = fieldRef;
        }

        /** The JSONPath reference into the data set (must resolve to exactly one value). */
        public FieldBinding from(String jsonPath) {
            if (jsonPath == null || !jsonPath.startsWith("$")) {
                throw new FormSpecException("jsonPath must start with '$': " + jsonPath);
            }
            this.jsonPath = jsonPath;
            return this;
        }

        public FieldBinding asText() {
            return asFormat("text", Map.of());
        }

        public FieldBinding asText(String caseMode) {
            return asFormat("text", Map.of("case", caseMode));
        }

        public FieldBinding asNumber() {
            return asFormat("number", Map.of());
        }

        public FieldBinding asNumber(int decimals) {
            return asFormat("number", Map.of("decimals", decimals));
        }

        public FieldBinding asCurrency() {
            return asFormat("currency", Map.of());
        }

        public FieldBinding asCurrency(int decimals) {
            return asFormat("currency", Map.of("decimals", decimals));
        }

        public FieldBinding asCurrency(String symbol, int decimals) {
            return asFormat("currency", Map.of("symbol", symbol, "decimals", decimals));
        }

        public FieldBinding asDate() {
            return asFormat("date", Map.of());
        }

        public FieldBinding asDate(String inputFormat, String outputFormat) {
            return asFormat("date", Map.of("inputFormat", inputFormat, "outputFormat", outputFormat));
        }

        public FieldBinding asSsn() {
            return asFormat("ssn", Map.of());
        }

        public FieldBinding asSsn(String separator) {
            return asFormat("ssn", Map.of("separator", separator));
        }

        public FieldBinding asCheckbox() {
            return asFormat("checkbox", Map.of());
        }

        public FieldBinding asCheckbox(Object checkedValue) {
            return asFormat("checkbox", Map.of("checkedValue", checkedValue));
        }

        public FieldBinding asFormat(String type) {
            return asFormat(type, Map.of());
        }

        public FieldBinding asFormat(String type, Map<String, Object> params) {
            this.format = new Format(type, params);
            return this;
        }

        public FieldBinding required() {
            this.required = true;
            return this;
        }

        public FieldBinding optional() {
            this.required = false;
            return this;
        }

        public FieldBinding withDescription(String description) {
            this.description = description;
            return this;
        }

        /** Offsets the value by (dx, dy) points relative to the field's rect. */
        public FieldBinding at(double dx, double dy) {
            this.dx = dx;
            this.dy = dy;
            return this;
        }

        /** Selective placement overrides merged over the field's template placement. */
        public FieldBinding withPlacement(PlacementOverride overrides) {
            this.overrides = overrides;
            return this;
        }

        /** Attaches a human note to the field (tooling, not printed). */
        public FieldBinding note(String note) {
            this.note = note;
            return this;
        }

        /** Draws a short mark on top of the field, e.g. "REVIEW". */
        public FieldBinding marked(String text) {
            return marked(text, 0.0, 0.0);
        }

        /** Draws a short mark offset by (dx, dy) points from the field's rect. */
        public FieldBinding marked(String text, double dx, double dy) {
            this.markText = text;
            this.markDx = dx;
            this.markDy = dy;
            return this;
        }

        /** Flags the field for reviewer attention. */
        public FieldBinding highlighted() {
            this.highlighted = true;
            return this;
        }

        /** Commits this entry and starts the next one. */
        public FieldBinding field(String nextFieldRef) {
            add();
            return binding.field(nextFieldRef);
        }

        public Binding build() {
            add();
            return binding.build();
        }

        public Binding build(FormTemplate template) {
            add();
            return binding.build(template);
        }

        public String toJson() {
            add();
            return binding.toJson();
        }

        /** Commits this entry once; terminal calls stay idempotent after that. */
        private void add() {
            if (added) {
                return;
            }
            added = true;
            if (jsonPath == null) {
                throw new FormSpecException("Field '" + fieldRef + "' has no jsonPath; call from(...) first");
            }
            Optional<PlacementOverride> placement = Optional.empty();
            if (dx != null || dy != null || overrides != null) {
                placement = Optional.of(new PlacementOverride(
                        dx != null ? dx : 0.0,
                        dy != null ? dy : 0.0,
                        overrides != null ? overrides.hAlign() : null,
                        overrides != null ? overrides.vAlign() : null,
                        overrides != null ? overrides.fontFamily() : null,
                        overrides != null ? overrides.fontSize() : null,
                        overrides != null ? overrides.overflow() : null,
                        overrides != null ? overrides.multiLine() : null));
            }
            Optional<EntryAnnotation> annotation = Optional.empty();
            if (note != null || markText != null || highlighted) {
                Optional<Mark> mark = Optional.empty();
                if (markText != null) {
                    Optional<PlacementOverride> markPlacement = Optional.empty();
                    if (markDx != null || markDy != null) {
                        markPlacement = Optional.of(new PlacementOverride(markDx != null ? markDx : 0.0,
                                markDy != null ? markDy : 0.0, null, null, null, null, null, null));
                    }
                    mark = Optional.of(new Mark(markText, markPlacement));
                }
                annotation = Optional.of(new EntryAnnotation(
                        Optional.ofNullable(note), mark, highlighted));
            }
            binding.add(new BindingEntry(fieldRef, jsonPath, Optional.ofNullable(format),
                    required, placement, annotation, Optional.ofNullable(description)));
        }
    }
}
