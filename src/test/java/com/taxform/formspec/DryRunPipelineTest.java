package com.taxform.formspec;

import com.taxform.formspec.fill.FillPlan;
import com.taxform.formspec.fill.FillPlanEntry;
import com.taxform.formspec.fill.FillStatus;
import com.taxform.formspec.fill.FormFiller;
import com.taxform.formspec.model.annotation.AnnotationDocument;
import com.taxform.formspec.model.binding.Binding;
import com.taxform.formspec.model.form.FormTemplate;
import com.taxform.formspec.parse.DocumentParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end fill over the shipped examples: nested resolution, formatting,
 * checkbox semantics, relative placement offsets, and the missing-data policy.
 */
class DryRunPipelineTest {

    private static FillPlan plan;

    @BeforeAll
    static void runPipeline() {
        FormTemplate template = DocumentParser.parseTemplate(
                Paths.get("examples/form-templates/f1040-2025.template.json"));
        Binding binding = DocumentParser.parseBinding(
                Paths.get("examples/bindings/f1040-2025.binding.json"), template);
        AnnotationDocument annotation = DocumentParser.parseAnnotationDocument(
                Paths.get("examples/annotations/f1040-2025.annotation.json"), template);
        String data = DocumentParser.readJson(Paths.get("examples/sample-data/taxpayer-1.json"));
        plan = FormFiller.fill(template, binding, annotation, data);
    }

    private FillPlanEntry entry(String fieldRef) {
        return plan.entries().stream()
                .filter(e -> e.fieldRef().equals(fieldRef))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no entry for " + fieldRef));
    }

    @Test
    void happyPathFillsValues() {
        assertEquals(FillStatus.FILLED, entry("l1a.wagesAmt").status());
        assertEquals("$123,400", entry("l1a.wagesAmt").formatted().orElseThrow());
        assertEquals(FillStatus.FILLED, entry("tp.ssn").status());
        assertEquals("123-45-6789", entry("tp.ssn").formatted().orElseThrow());
        assertEquals("Alexandra", entry("tp.nameFirst").formatted().orElseThrow());
        assertEquals("IL", entry("addr.state").formatted().orElseThrow());
        assertEquals("03/15/2026", entry("sig.date").formatted().orElseThrow());
        assertEquals("$147,840", entry("l11a.agiAmt").formatted().orElseThrow());
        assertEquals("$31,500", entry("l12e.standardOrItemizedAmt").formatted().orElseThrow());
        assertEquals("071000013", entry("l35b.routingNumber").formatted().orElseThrow());
        assertEquals("$0", entry("l37.amountOwedAmt").formatted().orElseThrow());
        assertEquals(0, entry("l1a.wagesAmt").pageIndex());
        assertEquals(1, entry("l35b.routingNumber").pageIndex());
    }

    @Test
    void checkboxesFollowCheckedValue() {
        assertEquals(FillStatus.FILLED, entry("fs.mfj").status());
        assertEquals("X", entry("fs.mfj").formatted().orElseThrow());
        assertEquals(FillStatus.SKIPPED_MISSING, entry("fs.single").status());
        assertTrue(entry("fs.single").note().orElseThrow().contains("not checked"));
        assertEquals(FillStatus.FILLED, entry("l35c.accountTypeChecking").status());
        assertEquals(FillStatus.SKIPPED_MISSING, entry("l35c.accountTypeSavings").status());
    }

    @Test
    void optionalAbsentPathIsSkippedNotFailed() {
        assertEquals(FillStatus.SKIPPED_MISSING, entry("l8.additionalIncomeAmt").status());
        assertTrue(entry("l8.additionalIncomeAmt").note().orElseThrow().contains("no value"));
    }

    @Test
    void relativePlacementOffsetsReachThePlan() {
        // sig.signature is annotated with placement {dx: -2, dy: -1}
        assertEquals(-2.0, entry("sig.signature").dx());
        assertEquals(-1.0, entry("sig.signature").dy());
        assertTrue(entry("sig.signature").hasOffset());
        // l1z.totalIncomeAmt has a font override but no offset
        assertEquals(0.0, entry("l1z.totalIncomeAmt").dx());
        assertFalse(entry("l1z.totalIncomeAmt").hasOffset());
        // fields without placement overrides carry zero offsets
        assertEquals(0.0, entry("tp.ssn").dx());
        assertFalse(entry("tp.ssn").hasOffset());
    }

    @Test
    void positionalAnnotationsReachThePlan() {
        assertEquals(4, plan.annotations().size());
        var helper = plan.annotations().get(0);
        assertEquals("tp.nameFirst", helper.fieldRef());
        assertEquals("left", helper.positioning().jsonValue());
        assertEquals("middle", helper.alignment().jsonValue());
        assertEquals("Add your name here", helper.text());
        assertEquals(4.0, plan.annotations().get(1).offset());
    }

    @Test
    void annotationsReachThePlan() {
        var interest = plan.bindingAnnotations().stream()
                .filter(a -> a.fieldRef().equals("l2b.taxableInterestAmt"))
                .findFirst().orElseThrow();
        assertEquals("Cross-check against Form 1099-INT before filing", interest.note().orElseThrow());

        var gain = plan.bindingAnnotations().stream()
                .filter(a -> a.fieldRef().equals("l7a.capitalGainAmt"))
                .findFirst().orElseThrow();
        assertEquals("REVIEW", gain.mark().orElseThrow().text());
        assertTrue(gain.highlight());

        assertEquals(1, plan.stamps().size());
        assertEquals("DRAFT — DO NOT FILE", plan.stamps().get(0).text());
        assertEquals(0, plan.stamps().get(0).page());
    }

    @Test
    void happyPathHasNoErrors() {
        Map<FillStatus, Long> counts = plan.entries().stream()
                .collect(Collectors.groupingBy(FillPlanEntry::status, Collectors.counting()));
        assertEquals(0L, counts.getOrDefault(FillStatus.ERROR_PATH, 0L));
        assertEquals(0L, counts.getOrDefault(FillStatus.ERROR_FORMAT, 0L));
        assertEquals(0L, counts.getOrDefault(FillStatus.ERROR_MISSING_REQUIRED, 0L));
        assertEquals(62, plan.entries().size());
    }

    @Test
    void requiredMissingPathIsAnError() {
        String data = """
                {
                  "filingStatus": "MFJ",
                  "taxpayer": { "ssn": "123456789" }
                }
                """;
        FormTemplate template = DocumentParser.parseTemplate(
                Paths.get("examples/form-templates/f1040-2025.template.json"));
        Binding binding = DocumentParser.parseBinding("""
                {
                  "specVersion": "1.3.0",
                  "kind": "binding",
                  "form": { "id": "f1040", "taxYear": 2025, "version": "2025-1" },
                  "fields": [
                    { "fieldRef": "tp.ssn", "jsonPath": "$.taxpayer.ssn", "required": true },
                    { "fieldRef": "tp.nameFirst", "jsonPath": "$.taxpayer.name.first", "required": true },
                    { "fieldRef": "tp.nameLast", "jsonPath": "$.taxpayer.name.last", "required": false }
                  ]
                }
                """, template);
        FillPlan result = FormFiller.fill(template, binding, data);
        assertEquals(FillStatus.FILLED, result.entries().get(0).status());
        assertEquals(FillStatus.ERROR_MISSING_REQUIRED, result.entries().get(1).status());
        assertEquals(FillStatus.SKIPPED_MISSING, result.entries().get(2).status());
    }

    @Test
    void unregisteredCustomFormatterIsAnError() {
        FormTemplate template = DocumentParser.parseTemplate(
                Paths.get("examples/form-templates/f1040-2025.template.json"));
        Binding binding = DocumentParser.parseBinding("""
                {
                  "specVersion": "1.3.0",
                  "kind": "binding",
                  "form": { "id": "f1040", "taxYear": 2025, "version": "2025-1" },
                  "fields": [
                    { "fieldRef": "tp.ssn", "jsonPath": "$.taxpayer.ssn",
                      "formatter": { "type": "x-not-registered" } }
                  ]
                }
                """, template);
        FillPlan result = FormFiller.fill(template, binding,
                DocumentParser.readJson(Paths.get("examples/sample-data/taxpayer-1.json")));
        assertEquals(FillStatus.ERROR_FORMAT, result.entries().get(0).status());
    }
}
