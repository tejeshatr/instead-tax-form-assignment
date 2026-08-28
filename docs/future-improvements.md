# Future Improvements

Living list of enhancements to the tax-form annotation project. Priorities are
relative to the three criteria the assignment is assessed on — **scope
accounted for**, **cleanliness**, and **walkthrough quality** — plus effort.
`spec.md` §12 lists further ideas from a spec-design angle.

---

## 1. More form templates, bindings, and annotation documents

**What**: ship additional form templates (e.g. `f1099-int`, `f1099-div`,
`f1099-nec`, `w2`), a **second binding** for the same 1040 template
against a *different* data source (e.g. a payroll engine's JSON shape), a
second annotation document, and a **second tax year**
(`f1040-2026.template.json`) once the IRS publishes it.

**Why**: this is the strongest possible proof of the three-artifact design —
one template serving many bindings and annotations, surviving template
version bumps. It directly grows "scope accounted for": every new form
exercises new field shapes (1099 boxes, W-2's numbered boxes, per-digit
sub-cells on some forms), which will surface spec gaps (see #4 re
sub-cells). Geometry can be measured with the same extraction flow used for
the shipped 1040 template.

**Effort**: low–medium per form (~an hour each with the extraction script).

## 2. Extraction tooling in the repo

**What**: productize the throwaway field-extraction scripts (PDF widget rects +
label matching via pymupdf/pypdf) as `tools/extract-template.py`, with a README
section on measuring a new form.

**Why**: makes #1 repeatable by anyone, including graders — turning a
one-off artifact into documented tooling strengthens "cleanliness" and gives
the walkthrough a second demo beat.

**Effort**: low (the logic already exists; it needs cleanup and docs).

## 3. CI on GitHub

**What**: a minimal GitHub Actions workflow running `./mvnw test` on push/PR.

**Why**: one file, and it turns "52 tests pass" from a claim into a
continuously verifiable fact. Obvious cleanliness signal.

**Effort**: trivial.

## 4. Reference PDF renderer

**What**: a PDFBox-based renderer that loads a blank form PDF, resolves the
binding, draws each value inside its rect per the placement rules, and layers
the annotation document on top — the literal scenario in the assignment
("build an application that can print values on top of the forms").

**Why**: the dry run proves everything *up to* drawing. A reference renderer
closes the loop and turns the walkthrough's fill plan into a picture of a
real, filled 1040. Also forces the spec to answer real rendering questions:
baseline positioning, autofit measurement, per-digit sub-cell fields (some
forms split SSN boxes into cells — the current spec has no concept of a field
being a group of sub-fields; the renderer will expose that gap immediately).

**Effort**: medium (PDFBox 3.x; Apache 2.0; text metrics are the fiddly part).

## 5. More formatters + locale support

**What**: `phone`, `ein`, `percent`, generic `mask`; `locale` param on
`number`/`currency`/`date` for non-US usage.

**Why**: cheap scope expansion; the registry's `x-` extension point already
shows how custom ones plug in, so a few more built-ins keep the core set
useful without bloating it.

**Effort**: low per formatter.

## 6. Conditional fields

**What**: render a field only when a predicate over the data holds (e.g. "print
Schedule B only if `taxableInterest > 1500`").

**Why**: real form packages are conditional. Needs a predicate subset — the
JSONPath filter syntax is a natural base. (Page-level watermarks are largely
covered by stamps since v1.2.0.)

**Effort**: medium.

---

## Non-goals (deliberately unchanged)

No taxpayer data model, no tax-calculation engine, no form-specific business
rules (the IRS MeF schemas own that space), no Java library beyond what the
spec itself needs — the reference implementation stays a reference, not a
product.
