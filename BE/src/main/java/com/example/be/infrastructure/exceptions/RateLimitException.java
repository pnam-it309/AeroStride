package com.example.be.infrastructure.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RateLimitException extends BusinessException {
    public RateLimitException(String message) {
        super(message);
    }
}
