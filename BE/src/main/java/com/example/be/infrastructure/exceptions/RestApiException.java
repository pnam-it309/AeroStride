package com.example.be.infrastructure.exceptions;

import org.springframework.http.HttpStatus;

public class RestApiException extends BusinessException {
    public RestApiException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
