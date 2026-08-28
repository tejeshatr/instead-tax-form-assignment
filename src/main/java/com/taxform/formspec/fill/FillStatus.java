package com.taxform.formspec.fill;

/** Outcome of resolving, formatting, and placing one annotation entry. */
public enum FillStatus {
    /** Value resolved and formatted; the renderer prints it inside the box. */
    FILLED,
    /** No value (or checkbox not checked); the box is left blank. */
    SKIPPED_MISSING,
    /** The entry is required but no value could be resolved. */
    ERROR_MISSING_REQUIRED,
    /** The JSONPath could not be resolved (malformed, or matched multiple values). */
    ERROR_PATH,
    /** The value could not be formatted by its formatter. */
    ERROR_FORMAT
}
