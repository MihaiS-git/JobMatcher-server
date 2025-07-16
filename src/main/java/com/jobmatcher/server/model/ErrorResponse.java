package com.jobmatcher.server.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse (
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        ErrorCode errorCode,
        Map<String, String> validationErrors
){
    public static ErrorResponse of(HttpStatus status, Object message, String path, ErrorCode errorCode) {
        String messageString = (message instanceof String) ? (String) message : null;
        Map<String, String> validationErrors = null;

        if (message instanceof Map<?, ?> map) {
            boolean isValid = map.keySet().stream().allMatch(k -> k instanceof String)
                    && map.values().stream().allMatch(v -> v instanceof String);
            if (isValid) {
                validationErrors = map.entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> (String) e.getKey(),
                                e -> (String) e.getValue()
                        ));
            }
        }

        return new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                messageString,
                path,
                errorCode,
                validationErrors
        );
    }
}