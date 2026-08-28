package com.taxform.formspec;

/**
 * All failures in the formspec model funnel into this runtime exception:
 * malformed documents, semantic violations (unknown field refs, out-of-bounds
 * rects), unresolvable JSONPaths, and formatting errors.
 */
public class FormSpecException extends RuntimeException {

    public FormSpecException(String message) {
        super(message);
    }

    public FormSpecException(String message, Throwable cause) {
        super(message, cause);
    }
}
