package com.taxform.formspec;

import com.networknt.schema.Error;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.Result;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaContext;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.taxform.formspec.parse.DocumentParser;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the shipped example documents conform to their JSON Schemas, and
 * that the schemas are not vacuous (a malformed document is rejected).
 */
class SchemaValidationTest {

    private static final Path TEMPLATE_SCHEMA = Paths.get("schema/form-template.schema.json");
    private static final Path BINDING_SCHEMA = Paths.get("schema/binding.schema.json");
    private static final Path ANNOTATION_SCHEMA = Paths.get("schema/annotation.schema.json");
    private static final Path TEMPLATE = Paths.get("examples/form-templates/f1040-2025.template.json");
    private static final Path BINDING = Paths.get("examples/bindings/f1040-2025.binding.json");
    private static final Path ANNOTATION = Paths.get("examples/annotations/f1040-2025.annotation.json");

    @Test
    void exampleTemplateConformsToSchema() {
        List<String> errors = validate(TEMPLATE_SCHEMA, DocumentParser.readJson(TEMPLATE));
        assertTrue(errors.isEmpty(), () -> "template failed schema: " + errors);
    }

    @Test
    void exampleBindingConformsToSchema() {
        List<String> errors = validate(BINDING_SCHEMA, DocumentParser.readJson(BINDING));
        assertTrue(errors.isEmpty(), () -> "annotation failed schema: " + errors);
    }

    @Test
    void malformedTemplateIsRejected() {
        String invalid = """
                {
                  "specVersion": "1.3.0",
                  "kind": "form-template",
                  "form": {
                    "id": "f1040",
                    "name": "U.S. Individual Income Tax Return",
                    "taxYear": 2025,
                    "version": "2025-1"
                  },
                  "pageSize": { "width": 612, "height": 792 },
                  "pages": [
                    {
                      "index": 0,
                      "fields": [
                        {
                          "id": "tp.nameFirst",
                          "name": "Your first name and middle initial",
                          "rect": { "x": 36.0, "y": 94.0, "width": 0, "height": 14.0 },
                          "placement": {
                            "hAlign": "left",
                            "vAlign": "middle",
                            "fontSize": { "mode": "fixed", "size": 9 },
                            "overflow": "truncate"
                          }
                        }
                      ]
                    }
                  ]
                }
                """;
        assertFalse(validate(TEMPLATE_SCHEMA, invalid).isEmpty(),
                "zero-width rect should fail the schema");
    }

    @Test
    void unknownFormatterTypeIsRejected() {
        String invalid = """
                {
                  "specVersion": "1.3.0",
                  "kind": "binding",
                  "form": { "id": "f1040", "taxYear": 2025, "version": "2025-1" },
                  "fields": [
                    {
                      "fieldRef": "tp.ssn",
                      "jsonPath": "$.taxpayer.ssn",
                      "formatter": { "type": "bogus" }
                    }
                  ]
                }
                """;
        assertFalse(validate(BINDING_SCHEMA, invalid).isEmpty(),
                "non-standard, non-x- formatter type should fail the schema");
    }

    @Test
    void exampleAnnotationDocumentConformsToSchema() {
        List<String> errors = validate(ANNOTATION_SCHEMA, DocumentParser.readJson(ANNOTATION));
        assertTrue(errors.isEmpty(), () -> "annotation document failed schema: " + errors);
    }

    @Test
    void wrongAxisAlignmentIsRejectedBySchema() {
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
        assertFalse(validate(ANNOTATION_SCHEMA, invalid).isEmpty(),
                "horizontal alignment on a vertical side should fail the schema");
    }

    @Test
    void emptyPlacementObjectIsRejected() {
        String invalid = """
                {
                  "specVersion": "1.3.0",
                  "kind": "binding",
                  "form": { "id": "f1040", "taxYear": 2025, "version": "2025-1" },
                  "fields": [
                    {
                      "fieldRef": "tp.ssn",
                      "jsonPath": "$.taxpayer.ssn",
                      "placement": {}
                    }
                  ]
                }
                """;
        assertFalse(validate(BINDING_SCHEMA, invalid).isEmpty(),
                "empty placement override should fail the schema");
    }

    /** Validates {@code json} against the 2020-12 schema in {@code schemaFile}; returns error messages. */
    private static List<String> validate(Path schemaFile, String json) {
        JsonMapper mapper = JsonMapper.builder().build();
        try {
            JsonNode schemaNode = mapper.readTree(DocumentParser.readJson(schemaFile));
            JsonNode instanceNode = mapper.readTree(json);
            SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
            SchemaContext context = new SchemaContext(
                    registry.getDialect(SpecificationVersion.DRAFT_2020_12.getDialectId()), registry);
            Schema schema = context.newSchema(SchemaLocation.DOCUMENT, schemaNode, null);
            ExecutionContext executionContext = new ExecutionContext();
            schema.validate(executionContext, instanceNode);
            return new Result(executionContext).getErrors().stream()
                    .map(Error::getMessage)
                    .toList();
        } catch (JacksonException e) {
            return List.of("schema engine failure: " + e.getMessage());
        } catch (RuntimeException e) {
            return List.of("schema engine failure: " + e.getMessage());
        }
    }
}
