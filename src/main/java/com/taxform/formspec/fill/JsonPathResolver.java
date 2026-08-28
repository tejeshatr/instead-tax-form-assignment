package com.taxform.formspec.fill;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JsonSmartJsonProvider;
import com.taxform.formspec.FormSpecException;

import java.util.List;
import java.util.Optional;

/**
 * Resolves a JSONPath reference against a JSON document. The only Jayway
 * touch-point in the codebase. Uses the bundled json-smart provider so no
 * additional JSON library is needed.
 *
 * <p>Per spec, a reference must resolve to exactly one value:
 * <ul>
 *   <li>no match or JSON {@code null} → {@link Optional#empty()} (missing)</li>
 *   <li>exactly one scalar → the value</li>
 *   <li>more than one (wildcard or filter match) → {@link FormSpecException}</li>
 * </ul>
 */
public final class JsonPathResolver {

    private static final Configuration CONFIG = Configuration.builder()
            .jsonProvider(new JsonSmartJsonProvider())
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build();

    private JsonPathResolver() {
    }

    public static Optional<Object> resolve(String json, String path) {
        JsonPath compiled;
        try {
            compiled = JsonPath.compile(path);
        } catch (Exception e) {
            throw new FormSpecException("Malformed JSONPath: " + path, e);
        }
        Object result = compiled.read(json, CONFIG);
        if (result == null) {
            return Optional.empty();
        }
        if (result instanceof List<?> list) {
            if (list.isEmpty()) {
                return Optional.empty();
            }
            if (list.size() > 1) {
                throw new FormSpecException("JSONPath resolved to " + list.size()
                        + " values (must resolve to exactly one): " + path);
            }
            return Optional.ofNullable(list.get(0));
        }
        return Optional.of(result);
    }
}
