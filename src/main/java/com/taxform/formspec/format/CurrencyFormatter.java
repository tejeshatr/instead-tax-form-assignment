package com.taxform.formspec.format;

import java.util.Map;

/**
 * Money amount.
 * <ul>
 *   <li>{@code symbol} — currency symbol, default {@code $}.</li>
 *   <li>{@code decimals} — 0–4, default 0.</li>
 *   <li>{@code grouping} — thousands separators, default {@code true}.</li>
 *   <li>{@code negativeParens} — render negatives as {@code (500)} instead of
 *       {@code -$500}, default {@code false}.</li>
 * </ul>
 */
public final class CurrencyFormatter implements Formatter {

    private final NumberFormatter number = new NumberFormatter();

    @Override
    public String format(Object raw, Map<String, Object> params) {
        String symbol = Params.str(params, "symbol", "$");
        boolean negativeParens = Params.bool(params, "negativeParens", false);
        String formatted = number.format(raw, params);
        if (formatted.startsWith("-")) {
            String magnitude = formatted.substring(1);
            return negativeParens ? "(" + magnitude + ")" : "-" + symbol + magnitude;
        }
        return symbol + formatted;
    }
}
