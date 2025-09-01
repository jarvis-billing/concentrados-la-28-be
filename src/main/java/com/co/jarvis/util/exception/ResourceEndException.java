package com.co.jarvis.util.exception;

public class ResourceEndException extends RuntimeException {

    public ResourceEndException() {
        super();
    }

    public ResourceEndException(String message) {
        super(message);
    }

    public ResourceEndException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceEndException(Throwable cause) {
        super(cause);
    }
}
