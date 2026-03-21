package com.example.be.infrastructure.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Thrown for internal system failures that should not leak details to the client
 * but require critical logging.
 */
@Getter
public class SystemException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public SystemException(String message) {
        this(message, "ERR_SYSTEM_INTERNAL", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public SystemException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
