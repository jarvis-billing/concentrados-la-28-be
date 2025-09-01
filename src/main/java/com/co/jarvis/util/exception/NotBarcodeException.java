package com.co.jarvis.util.exception;

public class NotBarcodeException extends RuntimeException {

    public NotBarcodeException() {
        super();
    }

    public NotBarcodeException(String message) {
        super(message);
    }

    public NotBarcodeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotBarcodeException(Throwable cause) {
        super(cause);
    }
}
