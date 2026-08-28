package com.taxform.formspec;

import com.taxform.formspec.model.binding.Binding;
import com.taxform.formspec.model.binding.Bindings;
import com.taxform.formspec.model.field.HAlign;
import com.taxform.formspec.model.field.PlacementOverride;
import com.taxform.formspec.model.form.FormTemplate;
import com.taxform.formspec.parse.DocumentParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves the fluent binding builder: chaining, validation, placement, annotations, and JSON round-trip. */
class BindingsTest {

    private static FormTemplate template;

    @BeforeAll
    static void loadTemplate() {
        template = DocumentParser.parseTemplate(
                Paths.get("examples/form-templates/f1040-2025.template.json"));
    }

    @Test
    void buildsChainedBinding() {
        Binding binding = Bindings.forForm("f1040", 2025, "2025-1")
                .fromDataSource("tax-engine", "application/json", "1.0.0")
                .field("l1a.wagesAmt").from("$.taxpayer.w2s[0].box1Wages").asCurrency().required()
                .field("fs.mfj").from("$.filingStatus").asCheckbox("MFJ").optional()
                .field("tp.ssn").from("$.taxpayer.ssn").asSsn()
                .build();

        assertEquals("binding", binding.kind());
        assertEquals("1.3.0", binding.specVersion());
        assertEquals("f1040", binding.form().id());
        assertEquals(2025, binding.form().taxYear());
        assertEquals("2025-1", binding.form().version());
        assertTrue(binding.dataSource().isPresent());
        assertEquals("tax-engine", binding.dataSource().get().name());
        assertEquals(3, binding.fields().size());

        var wages = binding.fields().get(0);
        assertEquals("l1a.wagesAmt", wages.fieldRef());
        assertEquals("$.taxpayer.w2s[0].box1Wages", wages.jsonPath());
        assertEquals("currency", wages.formatter().orElseThrow().type());
        assertTrue(wages.required());

        var filingStatus = binding.fields().get(1);
        assertEquals("checkbox", filingStatus.formatter().orElseThrow().type());
        assertEquals("MFJ", filingStatus.formatter().orElseThrow().params().get("checkedValue"));
        assertFalse(filingStatus.required());

        var ssn = binding.fields().get(2);
        assertEquals("ssn", ssn.formatter().orElseThrow().type());
        assertFalse(ssn.required());
    }

    @Test
    void buildsRelativePlacement() {
        Binding binding = Bindings.forForm("f1040", 2025, "2025-1")
                .field("sig.signature").from("$.signature.name").at(-2, -1)
                .field("l35d.accountNumber").from("$.refund.accountNumber")
                .withPlacement(new PlacementOverride(null, null, HAlign.RIGHT, null, null, null, null, null))
                .build();

        var signature = binding.fields().get(0);
        assertTrue(signature.placement().isPresent());
        assertEquals(-2.0, signature.placement().get().dx());
        assertEquals(-1.0, signature.placement().get().dy());
        assertEquals(null, signature.placement().get().hAlign());

        var account = binding.fields().get(1);
        assertEquals(HAlign.RIGHT, account.placement().orElseThrow().hAlign());
        assertEquals(0.0, account.placement().orElseThrow().dx());
    }

    @Test
    void buildsTrueAnnotations() {
        Binding binding = Bindings.forForm("f1040", 2025, "2025-1")
                .field("l2b.taxableInterestAmt").from("$.income.taxableInterest")
                .note("Cross-check against Form 1099-INT")
                .field("l7a.capitalGainAmt").from("$.income.capitalGain")
                .marked("REVIEW", 4, 0).highlighted()
                .build();

        var interest = binding.fields().get(0);
        assertTrue(interest.annotation().isPresent());
        assertEquals("Cross-check against Form 1099-INT", interest.annotation().get().note().orElseThrow());
        assertFalse(interest.annotation().get().mark().isPresent());
        assertFalse(interest.annotation().get().highlight());

        var gain = binding.fields().get(1);
        assertEquals("REVIEW", gain.annotation().orElseThrow().mark().orElseThrow().text());
        assertEquals(4.0, gain.annotation().get().mark().get().placement().orElseThrow().dx());
        assertEquals(0.0, gain.annotation().get().mark().get().placement().orElseThrow().dy());
        assertTrue(gain.annotation().get().highlight());
    }

    @Test
    void buildWithTemplateCrossChecksFieldRefs() {
        FormSpecException e = assertThrows(FormSpecException.class,
                () -> Bindings.forForm("f1040", 2025, "2025-1")
                        .field("tp.noSuchField").from("$.taxpayer.ssn")
                        .build(template));
        assertTrue(e.getMessage().contains("tp.noSuchField"));
    }

    @Test
    void toJsonRoundTripsThroughTheParser() {
        var binding = Bindings.forForm("f1040", 2025, "2025-1")
                .field("l1a.wagesAmt").from("$.taxpayer.w2s[0].box1Wages").asCurrency().required();
        Binding built = binding.build();
        Binding parsed = DocumentParser.parseBinding(binding.toJson(), template);
        assertEquals(built, parsed);
    }

    @Test
    void rejectsFieldWithoutJsonPath() {
        assertThrows(FormSpecException.class,
                () -> Bindings.forForm("f1040", 2025, "2025-1")
                        .field("tp.ssn")
                        .build());
    }

    @Test
    void rejectsDuplicateFieldRef() {
        assertThrows(FormSpecException.class,
                () -> Bindings.forForm("f1040", 2025, "2025-1")
                        .field("tp.ssn").from("$.a")
                        .field("tp.ssn").from("$.b")
                        .build());
    }

    @Test
    void rejectsNonJsonPathReference() {
        assertThrows(FormSpecException.class,
                () -> Bindings.forForm("f1040", 2025, "2025-1")
                        .field("tp.ssn").from("taxpayer.ssn"));
    }
}
