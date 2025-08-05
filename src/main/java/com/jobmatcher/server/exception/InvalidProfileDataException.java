package com.jobmatcher.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "Invalid profile data")
public class InvalidProfileDataException extends RuntimeException {
    public InvalidProfileDataException(String message) {
        super(message);
    }
}
