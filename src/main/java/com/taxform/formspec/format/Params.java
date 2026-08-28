package com.taxform.formspec.format;

import java.util.Map;

/** Parameter helpers shared by the built-in formatters. */
final class Params {

    private Params() {
    }

    static String str(Map<String, Object> params, String key, String defaultValue) {
        Object v = params.get(key);
        return v == null ? defaultValue : String.valueOf(v);
    }

    static boolean bool(Map<String, Object> params, String key, boolean defaultValue) {
        Object v = params.get(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(v));
    }

    static int integer(Map<String, Object> params, String key, int defaultValue) {
        Object v = params.get(key);
        if (v == null) {
            return defaultValue;
        }
        try {
            return v instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw new FormatException("Parameter '" + key + "' must be an integer, got: " + v);
        }
    }
}
