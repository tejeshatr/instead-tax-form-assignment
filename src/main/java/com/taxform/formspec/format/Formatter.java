package com.taxform.formspec.format;

import java.util.Map;

/**
 * Turns a resolved data value into the text a renderer prints inside the
 * box. Contract:
 * <ul>
 *   <li>Return the text to print, or {@code null} to leave the box blank
 *       (used by {@code checkbox} when its condition is not met).</li>
 *   <li>Throw {@link FormatException} when the value cannot be formatted.</li>
 * </ul>
 */
public interface Formatter {

    String format(Object raw, Map<String, Object> params);
}
