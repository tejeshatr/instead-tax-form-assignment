package com.taxform.formspec;

import com.taxform.formspec.fill.JsonPathResolver;
import com.taxform.formspec.parse.DocumentParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves the JSONPath subset behaves per spec: exactly-one-value semantics. */
class JsonPathResolverTest {

    private static String data;

    @BeforeAll
    static void load() {
        data = DocumentParser.readJson(Paths.get("examples/sample-data/taxpayer-1.json"));
    }

    @Test
    void resolvesDeeplyNestedArrayElement() {
        Optional<Object> result = JsonPathResolver.resolve(data, "$.taxpayer.w2s[0].box1Wages");
        assertTrue(result.isPresent());
        assertEquals(123400.0, result.get());
    }

    @Test
    void resolvesBracketQuotedKey() {
        Optional<Object> result = JsonPathResolver.resolve(data, "$['taxpayer']['ssn']");
        assertTrue(result.isPresent());
        assertEquals("123456789", result.get());
    }

    @Test
    void filterMatchingMultipleValuesThrows() {
        // The filter matches both W-2s → multi-value, which the spec forbids.
        assertThrows(FormSpecException.class,
                () -> JsonPathResolver.resolve(data, "$.taxpayer.w2s[?(@.year == 2025)].box1Wages"));
    }

    @Test
    void missingPathIsEmpty() {
        assertTrue(JsonPathResolver.resolve(data, "$.income.noSuchKey").isEmpty());
    }

    @Test
    void malformedPathThrows() {
        FormSpecException e = assertThrows(FormSpecException.class,
                () -> JsonPathResolver.resolve(data, "$[?(@.year = 2025)]"));
        assertTrue(e.getMessage().contains("Malformed JSONPath"));
    }

    @Test
    void wildcardMultiMatchThrows() {
        assertThrows(FormSpecException.class,
                () -> JsonPathResolver.resolve(data, "$.taxpayer.w2s[*].box1Wages"));
    }
}
