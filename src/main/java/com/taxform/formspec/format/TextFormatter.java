package com.taxform.formspec.format;

import java.util.Locale;
import java.util.Map;

/**
 * Plain text.
 * <ul>
 *   <li>{@code case} — {@code preserve} (default), {@code upper}, {@code lower}, {@code title}.</li>
 *   <li>{@code trim} — strip surrounding whitespace (default {@code true}).</li>
 * </ul>
 */
public final class TextFormatter implements Formatter {

    @Override
    public String format(Object raw, Map<String, Object> params) {
        String s = String.valueOf(raw);
        if (Params.bool(params, "trim", true)) {
            s = s.trim();
        }
        return switch (Params.str(params, "case", "preserve")) {
            case "upper" -> s.toUpperCase(Locale.ROOT);
            case "lower" -> s.toLowerCase(Locale.ROOT);
            case "title" -> titleCase(s);
            default -> s;
        };
    }

    /** Capitalize the first letter of each whitespace-separated word, leave the rest untouched. */
    private static String titleCase(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean startOfWord = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                out.append(c);
                startOfWord = true;
            } else if (startOfWord) {
                out.append(Character.toUpperCase(c));
                startOfWord = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
