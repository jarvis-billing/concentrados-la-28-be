package com.co.jarvis.util.exception;

public class GenericInternalException extends RuntimeException {

    public GenericInternalException(String message) {
        super(message);
    }

    public GenericInternalException(String message, Throwable cause) {
        super(message, cause);
    }
}
