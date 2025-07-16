package com.jobmatcher.server.exception;

public class PasswordResetException extends RuntimeException{
    public PasswordResetException(String message) {
        super(message);
    }
}
