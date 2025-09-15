package com.jobmatcher.server.exception;

public class InvalidProjectOperationException extends RuntimeException {
    public InvalidProjectOperationException(String message) {
        super(message);
    }
}