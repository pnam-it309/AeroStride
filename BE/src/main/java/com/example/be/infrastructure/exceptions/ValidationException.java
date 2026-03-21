package com.example.be.infrastructure.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Thrown for validation failures and user input errors.
 */
@Getter
public class ValidationException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public ValidationException(String message) {
        this(message, "ERR_VAL_BAD_REQUEST", HttpStatus.BAD_REQUEST);
    }

    public ValidationException(String message, String errorCode) {
        this(message, errorCode, HttpStatus.BAD_REQUEST);
    }

    private ValidationException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

}
