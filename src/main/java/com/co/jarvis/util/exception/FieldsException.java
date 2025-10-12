package com.co.jarvis.util.exception;

import java.util.List;
import java.util.Map;

public class FieldsException extends RuntimeException {

    private final Map<String, String> errors;

    public FieldsException() {
        super();
        this.errors = null;
    }

    public FieldsException(String message, List<String> fields) {
        super(message + String.join(", ", fields));
        this.errors = null;
    }

    public FieldsException(String message) {
        super(message);
        this.errors = null;
    }

    public FieldsException(String message, Throwable cause) {
        super(message, cause);
        this.errors = null;
    }

    public FieldsException(Throwable cause) {
        super(cause);
        this.errors = null;
    }

    public FieldsException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
