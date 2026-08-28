package com.taxform.formspec.format;

import com.taxform.formspec.FormSpecException;

/** Raised when a resolved value cannot be formatted by its formatter. */
public class FormatException extends FormSpecException {

    public FormatException(String message) {
        super(message);
    }

    public FormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
