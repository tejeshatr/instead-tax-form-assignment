package com.taxform.formspec.format;

import com.taxform.formspec.FormSpecException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Looks up formatters by their {@code type} name. Built-in types are fixed by
 * the spec ({@code text}, {@code number}, {@code currency}, {@code date},
 * {@code ssn}, {@code checkbox}); consumers extend the registry with custom
 * types, which must be prefixed {@code x-} or {@code custom-} to avoid
 * colliding with future built-ins.
 */
public final class FormatterRegistry {

    private static final Map<String, Formatter> BUILTINS = Map.of(
            "text", new TextFormatter(),
            "number", new NumberFormatter(),
            "currency", new CurrencyFormatter(),
            "date", new DateFormatter(),
            "ssn", new SsnFormatter(),
            "checkbox", new CheckboxFormatter());

    private static final Map<String, Formatter> CUSTOM = new ConcurrentHashMap<>();

    private FormatterRegistry() {
    }

    public static Optional<Formatter> get(String type) {
        Formatter builtin = BUILTINS.get(type);
        if (builtin != null) {
            return Optional.of(builtin);
        }
        return Optional.ofNullable(CUSTOM.get(type));
    }

    /** Registers a consumer-defined formatter; type must be {@code x-}/{@code custom-} prefixed. */
    public static void register(String type, Formatter formatter) {
        if (BUILTINS.containsKey(type)) {
            throw new FormSpecException("Formatter type is reserved: " + type);
        }
        if (!type.startsWith("x-") && !type.startsWith("custom-")) {
            throw new FormSpecException(
                    "Custom formatter types must start with 'x-' or 'custom-': " + type);
        }
        CUSTOM.put(type, formatter);
    }
}
