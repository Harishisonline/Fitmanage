package com.ironpump.fitmanage.exception;

/** Thrown on any field-level validation failure. The form shows the message inline. */
public class ValidationException extends FitManageException {
    private final String fieldName;

    public ValidationException(String fieldName, String userMessage) {
        super(userMessage);
        this.fieldName = fieldName;
    }

    public ValidationException(String userMessage) {
        super(userMessage);
        this.fieldName = null;
    }

    public String getFieldName() { return fieldName; }
}
