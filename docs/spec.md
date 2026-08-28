# Tax Form Annotation Specification — Version 1.3.0

## 1. Overview

This specification defines a data structure for **annotating the fields and boxes of a printed U.S. tax form**, so that any application can print values on top of the form inside each box. Two independent parties work together:

1. **A form annotator** describes a form by writing documents per this spec — where each box is, how values sit inside it, and which values go where.
2. **An application developer** builds a renderer that reads a binding plus a taxpayer's data set and prints each value inside its box. Their code needs no knowledge of any specific form.

The reference implementation in this repository (`com.taxform.formspec`) contains the Java model classes, a parser with semantic validation, the built-in formatters, and a dry-run filler that demonstrates the whole pipeline.

### Goals

- Precisely locate values on a fixed-layout form, in a renderer-independent way.
- Keep bindings free of geometry: authors work with **field names**, not coordinates.
- Reference any value in a **deeply nested data set** with a documented path syntax.
- Declare **formatting** (currency, dates, SSNs, checkboxes…) declaratively.
- Be implementable in any language by a conforming consumer.

### Non-goals (v1.3.0)

- Rendering itself — drawing is the consumer's proprietary code. This repository proves the spec with a dry run, not a PDF renderer.
- A taxpayer data model — the spec is agnostic to the shape of the data.
- Extracting geometry from official PDFs automatically — templates are authored documents (see §12 for tooling ideas).
- Computed values (e.g. summing lines) — every mapped value must exist in the data set (see §12).

## 2. Core concepts

### 2.1 Three documents

| Document | Kind | Written by | Contains | Written |
|---|---|---|---|---|
| **Form template** | `"form-template"` | Form annotator | Fields (boxes) with geometry and placement rules | Once per form + tax-year version |
| **Binding** | `"binding"` | Binding author | Binds template field ids to data paths, formatters, required flags, optional relative placement, and embedded annotations | Once per data source / consumer |
| **Annotation** | `"annotation"` | Annotation author | Presentation-only text positioned inside field boxes (side, alignment, offset) | Once per form / consumer |

The split exists because geometry is a property of the *form* (shared by everyone who fills it), while data bindings — and the way a given consumer places them — are a property of the *data and the application*. One template serves any number of bindings; one binding can be reused across tax years when the referenced fields still exist.

### 2.2 The fill pipeline

```
template + binding + annotation + data
        │
        ▼
 parse & validate          (DocumentParser — syntax, semantic rules, referential integrity)
        │
        ▼
 resolve each entry's      (JSONPath subset, §7 — exactly one scalar or error)
 data reference
        │
        ▼
 format the value          (built-in or registered formatter, §8)
        │
        ▼
 place the text inside     (rect + placement rules §4.4, merged with the entry's
 the box and print         relative placement §5.4 — the consumer's renderer)
        ▲
 layer annotations on top  (presentation-only document, §6 — side/alignment/offset)
```

A conforming renderer MUST: resolve paths with the semantics of §7, apply formatters per §8, place text per §4.4, honor the missing-data policy of §5.3, and layer annotation documents per §6.

## 3. Coordinate system and geometry

- **Units**: PDF points, 1/72 inch.
- **Origin**: the **top-left corner** of each page. `x` grows right; `y` grows down.
- **Pages**: addressed by a 0-based `index` in the template's `pages` array.
- **Page size**: declared once per template (`pageSize`); all field rects must lie within it. US Letter — the size of Form 1040 — is `612 x 792`.

A `rect` is the field's box: `{ "x": 504, "y": 450, "width": 72, "height": 12 }` means the box starts 504 points from the left edge and 450 points from the top edge of the page, and is 72 points wide and 12 tall.

> Renderers that draw in a PDF-native coordinate system (origin bottom-left) must flip the `y` axis: `y_pdf = pageHeight − (y + height)`.

## 4. The Form Template document

### 4.1 Shape

```json
{
  "specVersion": "1.3.0",
  "kind": "form-template",
  "form": {
    "id": "f1040",
    "name": "U.S. Individual Income Tax Return",
    "taxYear": 2025,
    "version": "2025-1",
    "officialAcroForm": "https://www.irs.gov/pub/irs-pdf/f1040.pdf"
  },
  "pageSize": { "width": 612, "height": 792 },
  "pages": [
    {
      "index": 0,
      "label": "Page 1",
      "fields": [ /* field objects, see §4.3 */ ]
    }
  ]
}
```

- `specVersion` — the version of this specification the document conforms to (`"1.3.0"`).
- `kind` — discriminator, always `"form-template"`.
- `form.id` — stable form identifier (`[a-z0-9][a-z0-9-]*`).
- `form.taxYear` — the tax year of this revision of the form.
- `form.version` — template version, e.g. `"2025-1"`. **Annotations reference this exact value** — an annotation written against a different template version is rejected (§9.2).
- `form.officialAcroForm` — optional URL of the official fillable PDF the geometry was measured from.
- `pageSize` — dimensions of every page.
- `pages[].index` — 0-based; indexes must be unique. `label` is for humans only.
- `pages[].fields[]` — the boxes, in any order.

### 4.2 Field object

```json
{
  "id": "l1a.wagesAmt",
  "name": "1a Total amount from Form(s) W-2, box 1",
  "description": "Optional note for annotators",
  "acroField": "f1_47",
  "rect": { "x": 504, "y": 450, "width": 72, "height": 12 },
  "placement": { "...": "see §4.4" }
}
```

- `id` — dotted, lowercase, unique within the template; segments are `[a-z][a-zA-Z0-9]*`. Inspired by IRS e-file naming, ids group by form area: `tp.ssn`, `sp.nameFirst`, `fs.mfj`, `l1a.wagesAmt`, `l35b.routingNumber`, `sig.date`.
- `acroField` — optional cross-reference to the field name in the official fillable PDF (e.g. `f1_47`). Purely informational; it lets a consumer hand values to the PDF's native form fields instead of drawing, and helps annotators verify geometry.

### 4.3 Placement rules

The template declares how values are placed inside each field's rect — annotations never carry positioning. The renderer computes the exact position:

| Property | Values | Meaning |
|---|---|---|
| `hAlign` | `left` \| `center` \| `right` | Horizontal alignment inside the rect |
| `vAlign` | `top` \| `middle` \| `bottom` | Vertical alignment inside the rect |
| `fontFamily` | `helvetica` \| `courier` \| `times` (default `helvetica`) | PDF-standard font; no embedding required |
| `fontSize` | `{ "mode": "fixed", "size": 9 }` or `{ "mode": "autofit", "min": 7, "max": 10 }` | Points. `autofit` shrinks the text until it fits the rect, bounded by `min`/`max` |
| `overflow` | `truncate` \| `error` | What happens when the value cannot fit: clip it, or fail the fill |
| `multiLine` | `true` \| `false` (default `false`) | Whether the value may wrap onto multiple lines |

Placement is declarative, not a pixel prescription: a conforming renderer measures the text at the chosen size (every PDF-standard font has well-defined metrics), aligns it per `hAlign`/`vAlign`, and applies `overflow`. Renderers that disagree on sub-pixel rounding still produce equivalent output because the rules — not the numbers — are the contract. (Note: `courier` is the traditional choice for right-aligned dollar amounts on tax forms.)

### 4.4 Validation rules for templates

Validated structurally by JSON Schema (`schema/form-template.schema.json`, strict: unknown properties are errors) and semantically by the parser:

- `specVersion` and `kind` must match this specification.
- Field ids must be unique across the whole template.
- Page indexes must be unique.
- Every rect must lie entirely within `pageSize`.
- `fontSize` params must be consistent with `mode` (§4.3).

## 5. The Binding document

### 5.1 Shape

```json
{
  "specVersion": "1.2.0",
  "kind": "binding",
  "form": { "id": "f1040", "taxYear": 2025, "version": "2025-1" },
  "dataSource": { "name": "tax-engine", "dataFormat": "application/json", "version": "1.0.0" },
  "fields": [
    {
      "fieldRef": "l1a.wagesAmt",
      "jsonPath": "$.taxpayer.w2s[0].box1Wages",
      "formatter": { "type": "currency" },
      "required": true,
      "placement": { "dx": 0, "dy": -1, "fontSize": { "mode": "fixed", "size": 10 } }
    }
  ]
}
```

- `form` identifies the template the binding was written against — `id`, `taxYear`, and `version` must all match the template (§9.2).
- `dataSource` — optional provenance of the data set the paths were written against.
- `fields[]` — one entry per bound field:
  - `fieldRef` — id of a field in the template. **The only link between binding and geometry.**
  - `jsonPath` — reference into the data set (§7).
  - `formatter` — optional; without one, the raw value is stringified (equivalent to `text` with defaults).
  - `required` — boolean, default `false` (§5.3).
  - `placement` — optional relative positioning for this consumer (§5.4).

For authoring bindings in code rather than by hand, the reference
implementation ships a fluent `Bindings` builder (see README) that emits
this exact JSON shape.

### 5.2 What a binding may not contain

Absolute coordinates, page numbers, and rects — absolute geometry lives in
the template only. Enforced by the schema (`additionalProperties: false`,
unknown keys are errors), and `placement` accepts only *relative*
properties: offsets and per-property overrides, never coordinates.

### 5.3 Missing-data policy

- **Missing** means: the path matches nothing, the value is JSON `null`, or the value is a string that is blank after trimming.
- `required: true` + missing → the fill **fails** with `ERROR_MISSING_REQUIRED`; the application should stop and report every failed entry.
- `required: false` + missing → the box is **left blank** (`SKIPPED_MISSING`). This is not an error.
- A checkbox whose `checkedValue` does not match is likewise left blank (§8.2).

A single boolean was chosen deliberately: a `missing: "skip" | "error"` enum would be redundant with `required` (see §11).

### 5.4 Relative placement

Each entry may declare `placement` — how this consumer positions the value
**relative to the field's box**:

| Property | Meaning |
|---|---|
| `dx` / `dy` | Offset in points from the field's rect (positive = right / down) |
| `hAlign`, `vAlign` | Alignment inside the (shifted) box |
| `fontFamily`, `fontSize` | Font of this value |
| `overflow`, `multiLine` | Fit policy of this value |

Every property is optional; anything unspecified **inherits the field's
placement from the form template** (§4.3). An entry without `placement`
prints exactly per the template; an empty `{}` is invalid (the schema
requires at least one property).

The layer exists because the same template + binding pair prints
differently for different consumers — screen previews, printers with
different font metrics, archival copies — without editing the shared
template. The renderer merges: `effective placement = entry placement
over the field's template placement`.

### 5.5 True annotations — notes, marks, highlights, stamps

Beyond wiring data, an entry may carry an `annotation` — something *extra*
added on top of the field:

| Property | Meaning | Printed? |
|---|---|---|
| `note` | Human comment attached to the field, e.g. "Cross-check against Form 1099-INT" | No — tooling only |
| `mark` | Short text drawn on top of the field, e.g. `{ "text": "REVIEW", "placement": { "dx": 4 } }` | Yes — positioned relative to the field's rect |
| `highlight` | Boolean flag for reviewer attention | Tooling; renderers may draw emphasis |

The document may also carry `stamps` — page-level overlay text such as
`DRAFT` or `AMENDED`, each with its own rect and placement:

```json
"stamps": [
  { "text": "DRAFT — DO NOT FILE", "page": 0,
    "rect": { "x": 450, "y": 30, "width": 110, "height": 14 },
    "placement": { "hAlign": "center", "vAlign": "middle",
                   "fontSize": { "mode": "fixed", "size": 10 },
                   "overflow": "truncate" } }
]
```

Stamps are document-level because they span a page, not a field. This is
what "annotation" means in this specification: additive, human-facing
content layered on top of the form — distinct from the binding, which
wires the data.

## 6. The Annotation document

Presentation-only text layered on top of a form. Each annotation is bound to
a template field and positioned **inside the field's box, relative to the
field itself** — hugging one of its sides — for human readability: helper
text ("Add your name here"), review marks ("REVIEW"), hints ("Direct
deposit").

```json
{
  "specVersion": "1.3.0",
  "kind": "annotation",
  "form": { "id": "f1040", "taxYear": 2025, "version": "2025-1" },
  "annotations": [
    { "field_ref": "tp.nameFirst", "positioning": "left", "alignment": "middle",
      "text": "Add your name here" },
    { "field_ref": "l7a.capitalGainAmt", "positioning": "right", "alignment": "middle",
      "text": "REVIEW", "offset": 4 }
  ]
}
```

- `field_ref` — template field id; must exist in the template (§10.2).
- `positioning` — which edge of the field's box the text hugs: `left | right | top | bottom`.
- `alignment` — where along that edge: `top | middle | bottom` for left/right sides; `left | center | right` for top/bottom sides.
- `text` — the content (printed).
- `offset` — optional gap in points from the edge (default 0).

Annotation text inherits the field's placement (font family and size); the
renderer computes the position from the field's rect + side + alignment +
offset. No data references, no absolute coordinates — purely additive
presentation, authored and shipped separately from the binding. The
schema's `if`/`then` rejects an alignment from the wrong axis, and the
parser validates `field_ref` existence.

## 7. Referencing values — the JSONPath subset

The spec defines a **subset** of JSONPath (RFC 9535). It is expressive enough for real tax data and small enough to document and implement faithfully:

| Construct | Example | Meaning |
|---|---|---|
| Root | `$` | The whole data document |
| Dot access | `$.taxpayer.ssn` | Property `ssn` of `taxpayer` |
| Bracket access | `$['taxpayer']['ssn']` | Same, for keys with odd characters |
| Array index | `$.taxpayer.w2s[0].box1Wages` | First element, 0-based |
| Recursive descent | `$..box1Wages` | Any `box1Wages` anywhere |
| Filter | `$.taxpayer.w2s[?(@.year == 2025)]` | Elements where `year` equals 2025 (operators: `==`, `!=`) |

**Single-value rule**: a reference must resolve to **exactly one value**. No match, or JSON `null` → missing (§5.3). More than one match (a wildcard or filter hitting several elements) → `ERROR_PATH`, and the fill fails. Tax forms have a fixed number of boxes; silently printing an arbitrary array into one box is never the right answer. (Multi-value joins are a future enhancement, §12.)

The subset is implemented with Jayway JsonPath 2.x in the reference code, which also implements the rest of RFC 9535's surface; **the subset above is the part this specification guarantees**. Consumers should implement at least this subset and are free to accept more.

## 8. Formatters

Formatters are declared on binding entries (never on template fields in v1.2 — see §11.2). A formatter is `{ "type": <name>, "params": { ... } }`.

### 7.1 Built-ins

| Type | Purpose | Params (defaults in bold) | `"123400"` → |
|---|---|---|---|
| `text` | Free text | `case`: **`preserve`** \| `upper` \| `lower` \| `title`; `trim`: **`true`** | `123400` |
| `number` | Plain number | `decimals`: 0–4, **`0`**; `grouping`: **`true`** | `123,400` |
| `currency` | Money | `symbol`: **`$`**; `decimals`: 0–4, **`0`**; `grouping`: **`true`**; `negativeParens`: **`false`** (`true` → `(500)`) | `$123,400` |
| `date` | Calendar date | `inputFormat`: **ISO `yyyy-MM-dd`**; `outputFormat`: **`MM/dd/yyyy`** (`java.time` patterns) | `03/15/2026` |
| `ssn` | U.S. Social Security Number | `separator`: **`-`**; non-digits in the input are ignored | `123-45-6789` |
| `checkbox` | Checkbox mark | `checkedValue`: **`true`**; `mark`: **`X`** | `X` iff value == checkedValue |

`title` case capitalizes the first letter of each whitespace-separated word and leaves the rest untouched (so `5B` stays `5B`).

Semantics of `checkbox`: the mark is drawn **iff** the resolved value equals `checkedValue` (compared as values, or as strings when types differ — `"MFJ"` equals `"MFJ"`, `true` equals `true`). A non-match is not an error: the box is left blank. This is how one annotation entry per filing-status box (`fs.single`, `fs.mfj`, …) checks exactly one box given a single `$.filingStatus` value.

### 7.2 Extension point

Consumers register custom formatters by type name. **Reserved**: the six built-in names. **Required prefix**: `x-` or `custom-` (e.g. `x-telephone`, `custom-ein`). The prefix rule keeps custom types from colliding with future built-ins. In the reference implementation: `FormatterRegistry.register(String type, Formatter formatter)`.

## 9. Validation and conformance

### 8.1 Structural

Both documents are validated against JSON Schemas (`schema/*.schema.json`, draft 2020-12, `additionalProperties: false` everywhere). Consequences:

- Typos in property names are rejected — a template `with` a `rects` typo does not silently produce an empty form.
- Formatter `params` are validated per type; unknown formatter types are rejected unless `x-`/`custom-` prefixed.

### 8.2 Semantic (parser)

Checks the schemas cannot express, enforced at parse time by `DocumentParser`:

- `specVersion` and `kind` match this specification.
- Template: unique field ids, unique page indexes, rects within page bounds (§4.4).
- Binding: `form.id`/`taxYear`/`version` equal the template's; every `fieldRef` exists in the template.

### 8.3 Conformance requirements for consumers

A conforming consumer MUST: (1) reject documents that fail §9.1/§9.2; (2) treat missing values per §5.3; (3) resolve paths per §7; (4) format per §8; (5) place text per §4.3 and honor `overflow`; (6) support all three PDF-standard fonts. It MAY draw to PDF, image, or any printable medium.

## 10. Worked example — Form 1040 (2025)

`examples/` ships a complete, real annotation of the official two-page 1040:

| File | What it is |
|---|---|
| `form-templates/f1040-2025.template.json` | 62 fields across both pages with **real geometry** (measured from the official `f1040.pdf`; `acroField` holds each field's native name) |
| `bindings/f1040-2025.binding.json` | A binding of all 62 fields against the `tax-engine` data source, with relative-placement and embedded-annotation demos |
| `annotations/f1040-2025.annotation.json` | A presentation-only annotation document: helper text, review mark, hints — positioned on field sides |
| `sample-data/taxpayer-1.json` | A deeply nested data set (two W-2s, spouse, refund details) |

Features demonstrated, and where to look:

- **Deep nesting + array index**: `l1a.wagesAmt ← $.taxpayer.w2s[0].box1Wages` → `$123,400`.
- **Formatting**: raw SSN `123456789` → `123-45-6789`; lowercase names/address → title case; `2026-03-15` → `03/15/2026`; whole-dollar currency.
- **Checkboxes**: five filing-status boxes, one `$.filingStatus` value — exactly one `X`. Same pattern for Checking/Savings.
- **Missing data**: `l8.additionalIncomeAmt` points at a key absent from the data set; `required: false` → box left blank. The test suite also proves the `required: true` error path.
- **Both pages**: page 0 carries identity + income; page 1 carries tax, payments, refund, signature.
- **Placement variety**: right-aligned `courier` for amounts, `autofit` on `addr.city`, `overflow: error` on the totals that must never be silently clipped, `multiLine` on `sig.occupation`.
- **Relative placement**: `sig.signature` nudged by `dx: -2, dy: -1`, `l1z.totalIncomeAmt` bumped to a 10pt fixed font, `l35d.accountNumber` right-aligned — three different override shapes over the template's defaults.
- **True annotations**: a note on `l2b.taxableInterestAmt`, a `REVIEW` mark + highlight on `l7a.capitalGainAmt`, and a `DRAFT — DO NOT FILE` stamp on page 0.
- **The annotation document**: "Add your name here" on the left edge of `tp.nameFirst`, `REVIEW` on the right edge of `l7a.capitalGainAmt` (offset 4), a "Direct deposit" hint above `l35b.routingNumber`, and a format hint below `sig.date`.

Run the dry run:

```
./mvnw test     # 31 tests: schema conformance, parser, paths, formatters, pipeline, demo
./mvnw -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt
java -cp "target/classes:$(cat target/cp.txt)" com.taxform.formspec.demo.DryRunDemo
```

The demo prints the fill plan — every entry with its page, rect, formatted value, and status. This output is the walkthrough video's centerpiece.

## 11. Design decisions and trade-offs

1. **Separate documents, not one merged document.** Geometry is per-form, bindings are per-data-source, and annotations are per-consumer. A single merged document would duplicate geometry across every consumer and make templates unreusable. The cost: three files and a `fieldRef` indirection — which is also the feature (binding and annotation authors never touch coordinates).
2. **Formatters live on the binding, not the template.** The same box is always a dollar box, tempting template-level defaults — but a template would then encode data semantics (a box could be `currency` or `date` depending on the year's form), and template-default-plus-annotation-override adds a precedence rule consumers must implement. Binding-only formatters plus one "no formatter = raw text" default is the smallest correct surface. Template defaults are deferred to §12.
3. **Top-left origin, points.** Matches how annotators measure and how form tools display coordinates; PDF's bottom-left origin is a renderer implementation detail handled by one formula (§3).
4. **A documented JSONPath subset, not the whole of RFC 9535.** The subset is implementable in a day in any language; the reference uses Jayway, whose dialect inspired the spec. Guaranteeing only the subset keeps conformance testable.
5. **Single-value rule.** One box, one value — multi-matches are errors, not guesses. Explicitly the conservative choice; joins are §12.
6. **One `required` boolean, no missing-policy enum.** `required` + "missing is skip-or-error" already covers both behaviors with fewer states to document and test.
7. **Strict schemas** (`additionalProperties: false`) so annotations fail loudly on typos instead of silently dropping geometry or format params.
8. **Flat records, no Jackson polymorphism.** `FontSize` is one record with nullable components rather than a sealed type hierarchy — smaller API, simpler serialization, and the semantic rules live in one compact constructor.
9. **No PDF renderer in the deliverable.** The assignment asks for a spec + classes; the dry-run resolver proves the spec end-to-end without a rendering dependency (PDFBox), keeping the library framework-free and license-clean. A conforming renderer can be built from §4.3/§7/§8 alone.
10. **Jackson 3-only runtime classpath.** Jayway is pinned to its bundled json-smart provider (`JsonSmartJsonProvider`) so no second JSON library ships. JSON Schema validation (networknt) is a **test-scope** dependency only.
11. **`acroField` cross-references** are optional metadata, not behavior — the spec never *requires* the official PDF to exist; geometry is always self-contained.
12. **Relative placement is a per-entry optional override, merged over the template at fill time.** Where a value sits *relative to* its box is a property of the consumer (different printers, fonts, previews), not of the form — so it lives on the binding entry, not the template, and stays optional so only fields that actually need adjustment carry it. An empty override is rejected; inheritance means entries never restate what the template already knows.

## 12. Future enhancements

In rough priority order — deliberately *not* half-implemented in v1.1.
(`docs/future-improvements.md` holds the project-level roadmap with effort
estimates.)

1. **Template-level default formatters** with binding override, once the precedence rules are worth the spec surface (see §11.2).
2. **Multi-value references and joins** — `$.taxpayer.w2s[*].box1Wages` with a `join: "sum"` or separator, enabling computed lines.
3. **Computed/derived fields** — declared arithmetic on template fields (`l1z = sum(l1a…l1h)`), moving tax-engine math into the spec.
4. **Conditional fields** — render a field only when a predicate on the data holds (page-level watermarks are largely covered by stamps since 1.2.0).
5. **Template diffing** — a tool (and spec note) for re-measuring a form between tax years and producing a minimal template update.
6. **Geometry extraction tooling** — auto-generate templates from official AcroForm/XFA rects (the shipped 1040 template was produced this way, semi-automatically).
7. **Barcode zones** — 2D-barcode placement for e-filed paper returns.
8. **Localization and multi-currency** — formatter `locale`, currency codes beyond `$`.
9. **Reverse extraction** — filled PDF → data, using the same documents as an overlay map.
10. **Reference renderer** — a PDFBox-based proof that prints onto the real 1040, as the natural follow-up to the dry run.

## 13. Glossary

- **Binding** — the fill-instruction document (§5): data references, formatting, required flags, optional relative placement, and true annotations; also, loosely, the act of writing one.
- **True annotation** — something extra added on top of a field: a note, a mark, or a highlight (§5.5).
- **Field** — a named box on the form; the unit of annotation. Geometry + placement live on the field.
- **Fill plan** — the dry-run output: for each entry, the resolved value, target box, relative offset, and status.
- **Form template** — the geometry document (§4).
- **Missing** — no match, JSON null, or blank string (§5.3).
- **Placement** — the declarative rules positioning a value inside a rect (§4.3).
- **Placement override** — an annotation entry's relative positioning: offset plus selective property overrides merged over the field's placement (§5.4).
- **Rect** — `{x, y, width, height}` in points, top-left origin (§3).
- **specVersion** — the version of this specification a document conforms to (`1.2.0`).
- **Stamp** — page-level overlay text, e.g. "DRAFT" (§5.5).
