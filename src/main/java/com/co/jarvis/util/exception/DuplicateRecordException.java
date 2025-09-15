package com.co.jarvis.util.exception;

import com.co.jarvis.util.exception.model.ErrorResponse;

public class DuplicateRecordException extends RuntimeException {

    private ErrorResponse errorResponse;

    public DuplicateRecordException(String message) {
        super(message);
        this.errorResponse = ErrorResponse.builder()
                .message(message)
                .status("DUPLICATE_RECORD")
                .code(409)
                .build();
    }

    public DuplicateRecordException(String message, Throwable cause) {
        super(message, cause);
        this.errorResponse = ErrorResponse.builder()
                .message(message)
                .status("DUPLICATE_RECORD")
                .code(409)
                .build();
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }
}

