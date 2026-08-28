package com.taxform.formspec;

import com.taxform.formspec.model.binding.Binding;
import com.taxform.formspec.model.form.FormTemplate;
import com.taxform.formspec.parse.DocumentParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves parsing and the semantic checks JSON Schema cannot express. */
class DocumentParserTest {

    private static final Path TEMPLATE = Paths.get("examples/form-templates/f1040-2025.template.json");
    private static final Path BINDING = Paths.get("examples/bindings/f1040-2025.binding.json");

    @Test
    void parsesExampleTemplate() {
        FormTemplate template = DocumentParser.parseTemplate(TEMPLATE);
        assertEquals("f1040", template.form().id());
        assertEquals(2025, template.form().taxYear());
        assertEquals(612.0, template.pageSize().width());
        assertEquals(792.0, template.pageSize().height());
        assertEquals(2, template.pages().size());
        Set<String> ids = new HashSet<>();
        template.pages().forEach(p -> p.fields().forEach(f -> ids.add(f.id())));
        assertEquals(62, ids.size());
        // Both pages carry fields; geometry stays inside the letter page.
        assertTrue(template.pages().stream().allMatch(p -> p.fields().stream()
                .allMatch(f -> f.rect().within(template.pageSize()))));
    }

    @Test
    void parsesExampleBindingAgainstTemplate() {
        Binding binding = DocumentParser.parseBinding(BINDING, DocumentParser.parseTemplate(TEMPLATE));
        assertEquals(62, binding.fields().size());
        assertTrue(binding.dataSource().isPresent());
        assertEquals("tax-engine", binding.dataSource().get().name());
        long placements = binding.fields().stream().filter(e -> e.placement().isPresent()).count();
        assertEquals(3, placements);
        long annotations = binding.fields().stream().filter(e -> e.annotation().isPresent()).count();
        assertEquals(2, annotations);
        assertEquals(1, binding.stamps().size());
    }

    @Test
    void rejectsBindingForDifferentForm() {
        String bindingJson = """
                {
                  "specVersion": "1.3.0",
                  "kind": "binding",
                  "form": { "id": "f1040", "taxYear": 2024, "version": "2025-1" },
                  "fields": [
                    { "fieldRef": "tp.ssn", "jsonPath": "$.taxpayer.ssn" }
                  ]
                }
                """;
        FormSpecException e = assertThrows(FormSpecException.class,
                () -> DocumentParser.parseBinding(bindingJson, DocumentParser.parseTemplate(TEMPLATE)));
        assertTrue(e.getMessage().contains("but template is"));
    }

    @Test
    void rejectsUnknownFieldRef() {
        String bindingJson = """
                {
                  "specVersion": "1.3.0",
                  "kind": "binding",
                  "form": { "id": "f1040", "taxYear": 2025, "version": "2025-1" },
                  "fields": [
                    { "fieldRef": "tp.noSuchField", "jsonPath": "$.taxpayer.ssn" }
                  ]
                }
                """;
        FormSpecException e = assertThrows(FormSpecException.class,
                () -> DocumentParser.parseBinding(bindingJson, DocumentParser.parseTemplate(TEMPLATE)));
        assertTrue(e.getMessage().contains("tp.noSuchField"));
    }

    @Test
    void rejectsOutOfBoundsRect() {
        String template = """
                {
                  "specVersion": "1.3.0",
                  "kind": "form-template",
                  "form": {
                    "id": "f1040", "name": "U.S. Individual Income Tax Return",
                    "taxYear": 2025, "version": "2025-1"
                  },
                  "pageSize": { "width": 612, "height": 792 },
                  "pages": [
                    {
                      "index": 0,
                      "fields": [
                        {
                          "id": "tp.nameFirst",
                          "name": "Your first name",
                          "rect": { "x": 600, "y": 94, "width": 200, "height": 14 },
                          "placement": {
                            "hAlign": "left", "vAlign": "middle",
                            "fontSize": { "mode": "fixed", "size": 9 },
                            "overflow": "truncate"
                          }
                        }
                      ]
                    }
                  ]
                }
                """;
        FormSpecException e = assertThrows(FormSpecException.class,
                () -> DocumentParser.parseTemplate(template));
        assertTrue(e.getMessage().contains("lies outside page size"));
    }

    @Test
    void rejectsUnsupportedSpecVersion() {
        String template = """
                {
                  "specVersion": "9.9.9",
                  "kind": "form-template",
                  "form": {
                    "id": "f1040", "name": "U.S. Individual Income Tax Return",
                    "taxYear": 2025, "version": "2025-1"
                  },
                  "pageSize": { "width": 612, "height": 792 },
                  "pages": []
                }
                """;
        assertThrows(FormSpecException.class, () -> DocumentParser.parseTemplate(template));
    }
}
