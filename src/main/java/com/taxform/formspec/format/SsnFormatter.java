package com.taxform.formspec.format;

import java.util.Map;

/**
 * U.S. Social Security Number.
 * <ul>
 *   <li>{@code separator} — default {@code -}. Non-digits in the input are
 *       ignored, so {@code 123456789}, {@code 123-45-6789}, and
 *       {@code 123 45 6789} all render as {@code 123-45-6789}.</li>
 * </ul>
 */
public final class SsnFormatter implements Formatter {

    @Override
    public String format(Object raw, Map<String, Object> params) {
        String separator = Params.str(params, "separator", "-");
        String digits = String.valueOf(raw).replaceAll("\\D", "");
        if (digits.length() != 9) {
            throw new FormatException("SSN must contain 9 digits, found " + digits.length()
                    + " in: " + raw);
        }
        return digits.substring(0, 3) + separator
                + digits.substring(3, 5) + separator
                + digits.substring(5);
    }
}
