package com.example.be.infrastructure.exceptions;

import org.springframework.http.HttpStatus;

public class ExcelProcessingException extends BusinessException {
    public ExcelProcessingException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ExcelProcessingException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
        initCause(cause);
    }
}
