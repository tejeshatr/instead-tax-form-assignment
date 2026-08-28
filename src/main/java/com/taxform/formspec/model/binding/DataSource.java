package com.taxform.formspec.model.binding;

import java.util.Optional;

/** Optional provenance of the data an annotation was written against. */
public record DataSource(String name, String dataFormat, Optional<String> version) {

    public DataSource {
        version = version == null ? Optional.empty() : version;
    }
}
