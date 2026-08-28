package com.taxform.formspec.format;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * Plain number.
 * <ul>
 *   <li>{@code decimals} — 0–4, default 0 (tax forms are whole dollars).</li>
 *   <li>{@code grouping} — thousands separators, default {@code true}.</li>
 * </ul>
 */
public final class NumberFormatter implements Formatter {

    @Override
    public String format(Object raw, Map<String, Object> params) {
        BigDecimal value = toBigDecimal(raw).setScale(
                Params.integer(params, "decimals", 0), RoundingMode.HALF_UP);
        boolean grouping = Params.bool(params, "grouping", true);
        int decimals = Math.max(0, Params.integer(params, "decimals", 0));
        StringBuilder pattern = new StringBuilder(grouping ? "#,##0" : "0");
        if (decimals > 0) {
            pattern.append('.').append("0".repeat(decimals));
        }
        DecimalFormat df = new DecimalFormat(pattern.toString());
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(value);
    }

    static BigDecimal toBigDecimal(Object raw) {
        if (raw instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        if (raw instanceof String s) {
            try {
                return new BigDecimal(s.trim());
            } catch (NumberFormatException e) {
                throw new FormatException("Value is not numeric: '" + s + "'", e);
            }
        }
        throw new FormatException("Value is not numeric: " + raw);
    }
}
