package com.co.jarvis.util.exception;

public class SaveRecordException extends RuntimeException {

    public SaveRecordException(String message) {
        super(message);
    }

    public SaveRecordException(String message, Throwable cause) {
        super(message, cause);
    }
}

