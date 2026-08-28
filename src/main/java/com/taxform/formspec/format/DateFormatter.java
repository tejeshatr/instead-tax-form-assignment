package com.taxform.formspec.format;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Calendar date.
 * <ul>
 *   <li>{@code inputFormat} — {@link DateTimeFormatter} pattern the value is
 *       in; default ISO-8601 {@code yyyy-MM-dd}.</li>
 *   <li>{@code outputFormat} — pattern to render, default {@code MM/dd/yyyy}.</li>
 * </ul>
 */
public final class DateFormatter implements Formatter {

    @Override
    public String format(Object raw, Map<String, Object> params) {
        String text = String.valueOf(raw).trim();
        String inputFormat = Params.str(params, "inputFormat", "");
        String outputFormat = Params.str(params, "outputFormat", "MM/dd/yyyy");
        try {
            LocalDate date = inputFormat.isEmpty()
                    ? LocalDate.parse(text)
                    : LocalDate.parse(text, DateTimeFormatter.ofPattern(inputFormat));
            return date.format(DateTimeFormatter.ofPattern(outputFormat));
        } catch (DateTimeParseException e) {
            throw new FormatException("Value is not a valid date: '" + text + "'", e);
        }
    }
}
