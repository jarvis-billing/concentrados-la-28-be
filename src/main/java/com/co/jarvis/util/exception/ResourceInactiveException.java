package com.co.jarvis.util.exception;

public class ResourceInactiveException extends RuntimeException {

    public ResourceInactiveException() {
        super();
    }

    public ResourceInactiveException(String message) {
        super(message);
    }

    public ResourceInactiveException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceInactiveException(Throwable cause) {
        super(cause);
    }
}
