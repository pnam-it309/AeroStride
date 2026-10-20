package com.example.be.infrastructure.exceptions;

import lombok.Getter;

@Getter
public class RateLimitException extends BusinessException {
    public RateLimitException(String message) {
        super(message);
    }
}
