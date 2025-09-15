package com.co.jarvis.util.exception;

import com.co.jarvis.util.exception.model.ErrorResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList().stream().collect(Collectors.joining(System.lineSeparator()));
        ;

        return new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(DeleteRecordException.class)
    public ResponseEntity<Object> handleDeleteRecordException(DeleteRecordException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }

    @ExceptionHandler(SaveRecordException.class)
    public ResponseEntity<Object> handleSaveRecordException(SaveRecordException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }

    @ExceptionHandler(DuplicateRecordException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleDuplicateRecordException(DuplicateRecordException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getErrorResponse());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUsuarioNotFoundException(UserNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NotBarcodeException.class)
    public ResponseEntity<String> handleNotBarcodeException(NotBarcodeException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(GenericInternalException.class)
    public ResponseEntity<Object> handleGenericInternalException(GenericInternalException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }

    @ExceptionHandler(FieldsException.class)
    public ResponseEntity<Object> handleFileException(FieldsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(PasswordUserBadException.class)
    public ResponseEntity<Object> handlePasswordBadException(PasswordUserBadException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(ResourceEndException.class)
    public ResponseEntity<Object> handlePasswordBadException(ResourceEndException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
