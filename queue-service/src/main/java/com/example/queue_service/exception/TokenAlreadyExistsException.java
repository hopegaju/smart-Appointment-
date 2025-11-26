package com.example.queue_service.exception;

public class TokenAlreadyExistsException extends RuntimeException {
    public TokenAlreadyExistsException(String message) {
        super(message);
    }
}
