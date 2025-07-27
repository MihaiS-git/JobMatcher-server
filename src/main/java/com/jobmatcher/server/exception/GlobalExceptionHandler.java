package com.jobmatcher.server.exception;

import com.jobmatcher.server.model.ErrorCode;
import com.jobmatcher.server.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation", ex);
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (msg1, msg2) -> msg1 // in case of duplicate keys
                ));
        return buildErrorResponse(errors, HttpStatus.BAD_REQUEST, request.getRequestURI(), ErrorCode.VALIDATION_FAILED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("Validation error. ", ex);
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildErrorResponse(errors, HttpStatus.BAD_REQUEST, request.getRequestURI(), ErrorCode.VALIDATION_FAILED);
    }

    @ExceptionHandler({HttpMessageConversionException.class})
    public ResponseEntity<ErrorResponse> handleConversionException(HttpMessageConversionException ex, HttpServletRequest request) {
        log.warn("Invalid enum or input type. ", ex);
        return buildErrorResponse(
                "Invalid input. Please check your request body.",
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.VALIDATION_FAILED
        );
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleInvalidEnumException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Invalid enum or input type. ", ex);
        String message = "Invalid input: " + ex.getMostSpecificCause().getMessage();
        return buildErrorResponse(message, HttpStatus.BAD_REQUEST, request.getRequestURI(), ErrorCode.VALIDATION_FAILED);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Invalid HTTP method. ", ex);
        return buildErrorResponse("HTTP method not allowed.", HttpStatus.METHOD_NOT_ALLOWED, request.getRequestURI(), ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex, HttpServletRequest request) {
        log.error("Database error occurred. ", ex);
        return buildErrorResponse("A database error occurred. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR, request.getRequestURI(), ErrorCode.DATABASE_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error.", ex);
        return buildErrorResponse("An unexpected error occurred. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR, request.getRequestURI(), ErrorCode.INTERNAL_ERROR);
    }

    @ExceptionHandler(InvalidAuthException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAuthException(InvalidAuthException ex, HttpServletRequest request) {
        log.error("Invalid authentication.", ex);
        return buildErrorResponse("Authentication failed. Please check your credentials and try again.", HttpStatus.UNAUTHORIZED, request.getRequestURI(), ErrorCode.AUTH_INVALID);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex, HttpServletRequest request) {
        log.error("Email conflict. ", ex);
        return buildErrorResponse("Email address already in use.", HttpStatus.CONFLICT, request.getRequestURI(), ErrorCode.EMAIL_EXISTS);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.error("Resource not found. ", ex);
        return buildErrorResponse("Resource not found.", HttpStatus.NOT_FOUND, request.getRequestURI(), ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(PasswordRecoveryException.class)
    public ResponseEntity<ErrorResponse> handlePasswordRecoveryException(PasswordRecoveryException ex, HttpServletRequest request) {
        log.warn("Invalid email. ", ex);
        return buildErrorResponse("Password recovery failed. Please check the email address and try again.", HttpStatus.NOT_FOUND, request.getRequestURI(), ErrorCode.PASSWORD_RECOVERY_FAILED);
    }

    @ExceptionHandler(PasswordResetException.class)
    public ResponseEntity<ErrorResponse> handlePasswordResetException(PasswordResetException ex, HttpServletRequest request) {
        log.warn("Password reset token has expired. ", ex);
        return buildErrorResponse("Password reset failed. Password reset token has expired. Please try again.", HttpStatus.NOT_FOUND, request.getRequestURI(), ErrorCode.PASSWORD_RECOVERY_FAILED);
    }

    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<ErrorResponse> handleEmailSendException(EmailSendException ex, HttpServletRequest request) {
        log.warn("Unable to send password recovery email. ", ex);
        return buildErrorResponse("Unable to send password recovery email.", HttpStatus.SERVICE_UNAVAILABLE, request.getRequestURI(), ErrorCode.EMAIL_SENDING_FAILED);
    }

    @ExceptionHandler(GmailApiException.class)
    public ResponseEntity<ErrorResponse> handleGmailApiException(GmailApiException ex, HttpServletRequest request) {
        log.warn("Gmail API error ({}): {}", ex.getStatusCode(), ex.getMessage());

        HttpStatus status = switch (ex.getStatusCode()) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401, 403 -> HttpStatus.UNAUTHORIZED;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_GATEWAY;
        };

        return buildErrorResponse("Gmail API error: " + ex.getMessage(), status, request.getRequestURI(), ErrorCode.GMAIL_API_ERROR);
    }

    @ExceptionHandler(TokenCreationException.class)
    public ResponseEntity<ErrorResponse> handleTokenCreationException(TokenCreationException ex, HttpServletRequest request) {
        log.error("Failed to create reset token after multiple attempts.", ex);
        return buildErrorResponse(
                "Failed to create reset token. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI(),
                ErrorCode.TOKEN_CREATION_FAILED
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(MultipartException ex, HttpServletRequest request) {
        Throwable cause = ex.getCause();
        if (cause instanceof MaxUploadSizeExceededException) {
            log.error("Failed to upload file. File size exceeds the maximum allowed size of 10MB.");
            return buildErrorResponse(
                    "File size exceeds the maximum allowed size of 10MB.",
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    request.getRequestURI(),
                    ErrorCode.FILE_UPLOAD_FAILED
            );
        }
        log.error("Multipart error: ", ex);
        return buildErrorResponse(
                "File upload failed. Please try again.",
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.FILE_UPLOAD_FAILED
        );
    }

    @ExceptionHandler(UploadFileException.class)
    public ResponseEntity<ErrorResponse> handleUploadFileException(UploadFileException ex, HttpServletRequest request) {
        log.warn("File upload failed. ", ex);
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request.getRequestURI(), ErrorCode.FILE_UPLOAD_FAILED);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(Object message, HttpStatus status, String path, ErrorCode errorCode) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message, path, errorCode));
    }
}
