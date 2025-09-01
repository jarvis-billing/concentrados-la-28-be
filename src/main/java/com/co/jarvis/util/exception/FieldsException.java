package com.co.jarvis.util.exception;

import org.apache.coyote.BadRequestException;

import java.util.List;

public class FieldsException extends BadRequestException {

    public FieldsException() {
        super();
    }

    public FieldsException(String message, List<String> fields) {
        super(message + String.join(", ", fields));
    }

    public FieldsException(String message) {
        super(message);
    }

    public FieldsException(String message, Throwable cause) {
        super(message, cause);
    }

    public FieldsException(Throwable cause) {
        super(cause);
    }
}
