package com.taxform.formspec;

import com.taxform.formspec.model.annotation.Alignment;
import com.taxform.formspec.model.annotation.AnnotationDocument;
import com.taxform.formspec.model.annotation.Positioning;
import com.taxform.formspec.model.form.FormTemplate;
import com.taxform.formspec.parse.DocumentParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves the annotation document: parsing, side/alignment rules, and template validation. */
class AnnotationDocumentTest {

    private static FormTemplate template;

    @BeforeAll
    static void loadTemplate() {
        template = DocumentParser.parseTemplate(
                Paths.get("examples/form-templates/f1040-2025.template.json"));
    }

    @Test
    void parsesExampleAnnotationDocument() {
        AnnotationDocument document = DocumentParser.parseAnnotationDocument(
                Paths.get("examples/annotations/f1040-2025.annotation.json"), template);
        assertEquals("annotation", document.kind());
        assertEquals("1.3.0", document.specVersion());
        assertEquals("f1040", document.form().id());
        assertEquals(4, document.annotations().size());

        var first = document.annotations().get(0);
        assertEquals("tp.nameFirst", first.fieldRef());
        assertEquals(Positioning.LEFT, first.positioning());
        assertEquals(Alignment.MIDDLE, first.alignment());
        assertEquals("Add your name here", first.text());
        assertEquals(0.0, first.offset());

        var review = document.annotations().get(1);
        assertEquals(Positioning.RIGHT, review.positioning());
        assertEquals(4.0, review.offset());

        var top = document.annotations().get(2);
        assertEquals(Positioning.TOP, top.positioning());
        assertEquals(Alignment.CENTER, top.alignment());

        var bottom = document.annotations().get(3);
        assertEquals(Positioning.BOTTOM, bottom.positioning());
        assertEquals(Alignment.RIGHT, bottom.alignment());
    }

    @Test
    void rejectsAlignmentFromTheWrongAxis() {
        // positioning "left" hugs a vertical edge; "center" is a horizontal alignment.
        String invalid = """
                {
                  "specVersion": "1.3.0",
                  "kind": "annotation",
                  "form": { "id": "f1040", "taxYear": 2025, "version": "2025-1" },
                  "annotations": [
                    { "field_ref": "tp.nameFirst", "positioning": "left",
                      "alignment": "center", "text": "Add your name here" }
                  ]
                }
                """;
        FormSpecException e = assertThrows(FormSpecException.class,
                () -> DocumentParser.parseAnnotationDocument(invalid, template));
        assertTrue(e.getMessage().contains("vertical alignment"));
    }

    @Test
    void rejectsUnknownFieldRef() {
        String invalid = """
                {
                  "specVersion": "1.3.0",
                  "kind": "annotation",
                  "form": { "id": "f1040", "taxYear": 2025, "version": "2025-1" },
                  "annotations": [
                    { "field_ref": "tp.noSuchField", "positioning": "left",
                      "alignment": "middle", "text": "nope" }
                  ]
                }
                """;
        FormSpecException e = assertThrows(FormSpecException.class,
                () -> DocumentParser.parseAnnotationDocument(invalid, template));
        assertTrue(e.getMessage().contains("tp.noSuchField"));
    }

    @Test
    void rejectsDocumentForDifferentForm() {
        String invalid = """
                {
                  "specVersion": "1.3.0",
                  "kind": "annotation",
                  "form": { "id": "f1040", "taxYear": 2024, "version": "2025-1" },
                  "annotations": [
                    { "field_ref": "tp.nameFirst", "positioning": "left",
                      "alignment": "middle", "text": "Add your name here" }
                  ]
                }
                """;
        assertThrows(FormSpecException.class,
                () -> DocumentParser.parseAnnotationDocument(invalid, template));
    }
}
