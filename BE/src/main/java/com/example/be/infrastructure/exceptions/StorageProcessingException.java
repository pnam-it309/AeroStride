package com.example.be.infrastructure.exceptions;

public class StorageProcessingException extends RuntimeException {
    public StorageProcessingException(String message) {
        super(message);
    }

    public StorageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
