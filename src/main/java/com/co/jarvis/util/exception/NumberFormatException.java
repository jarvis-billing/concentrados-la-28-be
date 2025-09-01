package com.co.jarvis.util.exception;

public class NumberFormatException extends RuntimeException {

    public NumberFormatException(String message) {
        super(message);
    }

    public NumberFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

