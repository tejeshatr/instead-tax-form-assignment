package com.taxform.formspec.format;

import java.util.Map;
import java.util.Objects;

/**
 * Checkbox: the mark is drawn when the resolved value equals
 * {@code checkedValue}.
 * <ul>
 *   <li>{@code checkedValue} — value that checks the box; default
 *       {@code true} (so a boolean field works without params).</li>
 *   <li>{@code mark} — single character to draw, default {@code X}.</li>
 * </ul>
 * Returns {@code null} (box left blank) when the value does not match —
 * see the {@link Formatter} contract.
 */
public final class CheckboxFormatter implements Formatter {

    @Override
    public String format(Object raw, Map<String, Object> params) {
        Object checkedValue = params.getOrDefault("checkedValue", Boolean.TRUE);
        String mark = Params.str(params, "mark", "X");
        boolean checked = Objects.equals(raw, checkedValue)
                || String.valueOf(raw).equals(String.valueOf(checkedValue));
        return checked ? mark : null;
    }
}
