package com.jobmatcher.server.exception;

import lombok.Getter;

@Getter
public class GmailApiException extends RuntimeException {
    private final int statusCode;

    public GmailApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}
