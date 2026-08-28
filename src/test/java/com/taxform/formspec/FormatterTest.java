package com.taxform.formspec;

import com.taxform.formspec.format.FormatException;
import com.taxform.formspec.format.Formatter;
import com.taxform.formspec.format.FormatterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Table-driven proof of every built-in formatter and the registry contract. */
class FormatterTest {

    private static final Formatter TEXT = FormatterRegistry.get("text").orElseThrow();
    private static final Formatter NUMBER = FormatterRegistry.get("number").orElseThrow();
    private static final Formatter CURRENCY = FormatterRegistry.get("currency").orElseThrow();
    private static final Formatter DATE = FormatterRegistry.get("date").orElseThrow();
    private static final Formatter SSN = FormatterRegistry.get("ssn").orElseThrow();
    private static final Formatter CHECKBOX = FormatterRegistry.get("checkbox").orElseThrow();

    @Test
    void textCasesAndTrim() {
        assertEquals("Alexandra", TEXT.format("  alexandra  ", Map.of("case", "title")));
        assertEquals("IL", TEXT.format("il", Map.of("case", "upper")));
        assertEquals("rivera", TEXT.format("Rivera", Map.of("case", "lower")));
        assertEquals("preserved", TEXT.format("  preserved  ", Map.of()));
        assertEquals("  kept ", TEXT.format("  kept ", Map.of("trim", false)));
        assertEquals("5B", TEXT.format("5B", Map.of("case", "title")));
    }

    @Test
    void numberGroupingAndDecimals() {
        assertEquals("123,400", NUMBER.format(123400.0, Map.of()));
        assertEquals("123,456.79", NUMBER.format(123456.789, Map.of("decimals", 2)));
        assertEquals("123,457", NUMBER.format(123456.789, Map.of("decimals", 0)));
        assertEquals("-42", NUMBER.format(-42L, Map.of()));
        assertEquals("123400", NUMBER.format(123400.0, Map.of("grouping", false)));
        assertEquals("123,400", NUMBER.format("123400.0", Map.of()));
        assertThrows(FormatException.class, () -> NUMBER.format("nope", Map.of()));
    }

    @Test
    void currencyDefaultsAndNegativeStyles() {
        assertEquals("$123,400", CURRENCY.format(123400.0, Map.of()));
        assertEquals("-$500", CURRENCY.format(-500.0, Map.of()));
        assertEquals("(500)", CURRENCY.format(-500.0, Map.of("negativeParens", true)));
        assertEquals("€1,234", CURRENCY.format(1234, Map.of("symbol", "€")));
        assertEquals("$1,234.50", CURRENCY.format(1234.5, Map.of("decimals", 2)));
    }

    @Test
    void datesIsoAndCustomPatterns() {
        assertEquals("03/15/2026", DATE.format("2026-03-15", Map.of()));
        assertEquals("2026-03-15",
                DATE.format("15/03/2026", Map.of("inputFormat", "dd/MM/yyyy", "outputFormat", "yyyy-MM-dd")));
        assertThrows(FormatException.class, () -> DATE.format("not-a-date", Map.of()));
    }

    @Test
    void ssnNormalizesInputForms() {
        assertEquals("123-45-6789", SSN.format("123456789", Map.of()));
        assertEquals("123-45-6789", SSN.format("123-45-6789", Map.of()));
        assertEquals("123 45 6789", SSN.format(123456789, Map.of("separator", " ")));
        assertThrows(FormatException.class, () -> SSN.format("12345", Map.of()));
    }

    @Test
    void checkboxCheckedValueSemantics() {
        assertEquals("X", CHECKBOX.format("MFJ", Map.of("checkedValue", "MFJ")));
        assertNull(CHECKBOX.format("Single", Map.of("checkedValue", "MFJ")));
        assertEquals("X", CHECKBOX.format(true, Map.of()));
        assertNull(CHECKBOX.format(false, Map.of()));
        assertEquals("✓", CHECKBOX.format("checking", Map.of("checkedValue", "checking", "mark", "✓")));
    }

    @Test
    void registryExtensionContract() {
        FormatterRegistry.register("x-upper", (raw, params) -> String.valueOf(raw).toUpperCase());
        assertEquals("LOUD", FormatterRegistry.get("x-upper").orElseThrow().format("loud", Map.of()));
        assertThrows(FormSpecException.class,
                () -> FormatterRegistry.register("text", (r, p) -> String.valueOf(r)));
        assertThrows(FormSpecException.class,
                () -> FormatterRegistry.register("upper", (r, p) -> String.valueOf(r)));
        assertTrue(FormatterRegistry.get("x-no-such").isEmpty());
    }
}
