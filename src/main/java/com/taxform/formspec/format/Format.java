package com.taxform.formspec.format;

import java.util.Map;

/**
 * A declarative formatter reference: a built-in type ({@code text},
 * {@code number}, {@code currency}, {@code date}, {@code ssn},
 * {@code checkbox}) or a consumer-registered type prefixed {@code x-} /
 * {@code custom-}, plus its parameters. Parameter defaults are defined per
 * formatter in docs/spec.md.
 */
public record Format(String type, Map<String, Object> params) {

    public Format {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
