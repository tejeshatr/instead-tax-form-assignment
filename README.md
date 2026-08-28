# Tax Form Annotation Specification

A specification — with a Java reference implementation — for **annotating the fields and boxes of U.S. tax forms**, so that any application can print values on top of a form inside each box.

The deliverable:

| Artifact | Where |
|---|---|
| **The specification** | [`docs/spec.md`](docs/spec.md) — positioning, placement, formatting, value referencing, validation, design decisions, future work |
| **JSON Schemas** | [`schema/form-template.schema.json`](schema/form-template.schema.json), [`schema/binding.schema.json`](schema/binding.schema.json), [`schema/annotation.schema.json`](schema/annotation.schema.json) (draft 2020-12, strict) |
| **Java model + pipeline** | `src/main/java/com/taxform/formspec/` — framework-free classes (records + enums), formatters, parser, dry-run filler |
| **Worked example** | `examples/` — a real 62-field binding of Form 1040 (2025) plus a separate annotation document, with measured geometry and a deeply nested sample data set |
| **Future improvements** | [`docs/future-improvements.md`](docs/future-improvements.md) — prioritized roadmap |

## The idea in one paragraph

Three documents. A **form template** describes a form's boxes — id, page, rect, and placement rules — written once per form/tax-year. A **binding** wires template field ids to values in a data set via a documented JSONPath subset, plus a formatter, a required flag, optional *relative* placement (offset from the box + selective overrides), plus embedded notes/marks/highlights and page stamps. A separate **annotation** document layers presentation-only text inside field boxes — which side it hugs (`left|right|top|bottom`), where along that side, and an optional offset — for human readability. Bindings and annotations contain **no absolute coordinates**: a consumer's renderer resolves each path, formats the value, and places it inside the field's rect per the placement rules — with the binding's overrides merged on top, and annotations layered over the form. Same template, any number of bindings and annotations, any renderer.

Bindings can also be authored from Java code — the fluent builder mirrors the
JSON shape and can cross-check field ids against a template:

```java
Binding binding = Bindings.forForm("f1040", 2025, "2025-1")
        .field("l1a.wagesAmt").from("$.taxpayer.w2s[0].box1Wages").asCurrency().required()
        .field("fs.mfj").from("$.filingStatus").asCheckbox("MFJ").optional()
        .field("l7a.capitalGainAmt").from("$.income.capitalGain").marked("REVIEW").highlighted()
        .field("sig.signature").from("$.signature.name").at(-2, -1)
        .build(template); // optional fieldRef cross-check; .toJson() saves spec JSON
```

## Quick start

Requires JDK 21.

```sh
export JAVA_HOME="$(/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home 2>/dev/null || echo .)" # or your JDK 21
./mvnw test      # 52 tests — schema conformance, parsing, paths, formatters, end-to-end dry run
```

Print the dry-run fill plan (what the walkthrough video shows) — no arguments
needed; the demo loads the template, binding, annotation document, and sample
data from `examples/`:

```sh
./mvnw -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "target/classes:$(cat target/cp.txt)" com.taxform.formspec.demo.DryRunDemo
```

## Layout

```
docs/
  spec.md                       # THE specification
  future-improvements.md        # prioritized roadmap
schema/
  form-template.schema.json     # JSON Schema for templates
  binding.schema.json           # JSON Schema for bindings
  annotation.schema.json        # JSON Schema for annotation documents
examples/
  form-templates/f1040-2025.template.json   # 62 fields, real geometry (official f1040.pdf)
  bindings/f1040-2025.binding.json          # binding of all 62 fields + placement demos
  annotations/f1040-2025.annotation.json      # presentation-only annotations on field sides
  sample-data/taxpayer-1.json               # deeply nested data set
src/main/java/com/taxform/formspec/
  model/form/    # FormTemplate, FormMeta, FormRef
  model/page/    # FormPage, PageSize
  model/field/   # Field, Rect, Placement, PlacementOverride, FontSize + placement enums
  model/binding/  # Binding, BindingEntry, EntryAnnotation, Mark, Stamp, DataSource, Bindings (fluent builder)
  model/annotation/ # Annotation, AnnotationDocument, Positioning, Alignment
  format/        # Format, six formatters, FormatterRegistry, FormatException
  parse/         # DocumentParser — strict parsing + semantic validation
  fill/          # FormFiller, JsonPathResolver, FillPlan, FillPlanEntry, FillAnnotation, FillStatus
src/demo/java/com/taxform/formspec/demo/   # DryRunDemo — a consumer-style example app
src/test/java/com/taxform/formspec/        # 9 test classes, 51 tests
```

The Spring Boot application class remains an untouched scaffold — the library is framework-free by design: a consumer of this spec should not need Spring.

## Reference implementation notes

- **Runtime dependencies**: Jayway JsonPath (pinned to its bundled json-smart provider) and Jackson 3 — no Jackson 2, no PDF library.
- **JSON Schema validation** (networknt) is test-scoped only.
- Parsing is strict: unknown properties, out-of-bounds rects, unknown field refs, and mismatched template versions are all rejected with precise messages.

See the [spec](docs/spec.md) §10 for the full record of design decisions and §11 for future enhancements.
