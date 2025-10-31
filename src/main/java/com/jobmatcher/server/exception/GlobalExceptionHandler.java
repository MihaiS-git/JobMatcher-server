package com.jobmatcher.server.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.jobmatcher.server.model.ErrorCode;
import com.jobmatcher.server.model.ErrorResponse;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;


import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.INVALID_OPERATION
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request.getRequestURI(), ErrorCode.VALIDATION_FAILED);
    }

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

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ErrorResponse> handleTransactionSystemException(TransactionSystemException ex, HttpServletRequest request) {
        log.warn("Transaction system exception", ex);
        Map<String, String> errors = new HashMap<>();

        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException validationEx) {
                errors = validationEx.getConstraintViolations().stream()
                        .collect(Collectors.toMap(
                                v -> v.getPropertyPath().toString(),
                                ConstraintViolation::getMessage,
                                (msg1, msg2) -> msg1
                        ));
                break;
            }
            cause = cause.getCause();
        }

        if (errors.isEmpty()) {
            errors.put("error", ex.getMessage());
        }

        return buildErrorResponse(errors, HttpStatus.BAD_REQUEST, request.getRequestURI(), ErrorCode.VALIDATION_FAILED);
    }

    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<ErrorResponse> handlePersistenceException(PersistenceException ex, HttpServletRequest request) {
        log.warn("Persistence exception", ex);

        Map<String, String> errors = new HashMap<>();

        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof jakarta.validation.ConstraintViolationException validationEx) {
                errors = validationEx.getConstraintViolations().stream()
                        .collect(Collectors.toMap(
                                v -> v.getPropertyPath().toString(),
                                ConstraintViolation::getMessage,
                                (msg1, msg2) -> msg1
                        ));
                break;
            }
            cause = cause.getCause();
        }

        if (errors.isEmpty()) {
            errors.put("error", ex.getMessage());
        }

        return buildErrorResponse(errors, HttpStatus.BAD_REQUEST, request.getRequestURI(), ErrorCode.VALIDATION_FAILED);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error. ", ex);
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Invalid input received.", ex);

        Throwable rootCause = ex.getMostSpecificCause();
        String friendlyMessage;

        if (rootCause instanceof DateTimeParseException) {
            friendlyMessage = "Invalid date format. Please use yyyy-MM-dd.";
        } else if (rootCause instanceof InvalidFormatException) {
            // handles enum or type mismatch
            friendlyMessage = "Invalid input value. Please check the data types and allowed values.";
        } else {
            friendlyMessage = "Malformed request body.";
        }

        return buildErrorResponse(friendlyMessage, HttpStatus.BAD_REQUEST, request.getRequestURI(), ErrorCode.VALIDATION_FAILED);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Invalid HTTP method. ", ex);
        return buildErrorResponse("HTTP method not allowed.", HttpStatus.METHOD_NOT_ALLOWED, request.getRequestURI(), ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex, HttpServletRequest request) {
        log.warn("Database error occurred. ", ex);
        return buildErrorResponse("A database error occurred. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR, request.getRequestURI(), ErrorCode.DATABASE_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        Throwable rootCause = getRootCause(ex);
        log.warn("Unhandled exception: {}", ex.getMessage());
        return buildErrorResponse(
                rootCause.getMessage() != null ? rootCause.getMessage() : "Unexpected error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI(),
                ErrorCode.INTERNAL_ERROR
        );
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return getRootCause(cause);
        }
        return throwable;
    }

    @ExceptionHandler(InvalidAuthException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAuthException(InvalidAuthException ex, HttpServletRequest request) {
        log.warn("Invalid authentication.", ex);
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED, request.getRequestURI(), ErrorCode.AUTH_INVALID);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("Email conflict. ", ex);
        return buildErrorResponse("Email address already in use.", HttpStatus.CONFLICT, request.getRequestURI(), ErrorCode.EMAIL_EXISTS);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found. ", ex);
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request.getRequestURI(), ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(InvalidProjectOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProjectOperationException(InvalidProjectOperationException ex, HttpServletRequest request) {
        log.warn("Invalid project operation. ", ex);
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request.getRequestURI(), ErrorCode.INVALID_PROJECT_OPERATION);
    }

    @ExceptionHandler(PasswordRecoveryException.class)
    public ResponseEntity<ErrorResponse> handlePasswordRecoveryException(PasswordRecoveryException ex, HttpServletRequest request) {
        log.warn("Invalid email. ", ex);
        return buildErrorResponse("Password recovery failed. Please check the email address and try again.", HttpStatus.NOT_FOUND, request.getRequestURI(), ErrorCode.PASSWORD_RECOVERY_FAILED);
    }

    @ExceptionHandler(PasswordResetException.class)
    public ResponseEntity<ErrorResponse> handlePasswordResetException(PasswordResetException ex, HttpServletRequest request) {
        log.warn("Password reset token has expired. ", ex);
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request.getRequestURI(), ErrorCode.PASSWORD_RECOVERY_FAILED);
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
            default -> HttpStatus.valueOf(ex.getStatusCode());
        };

        return buildErrorResponse("Gmail API error: " + ex.getMessage(), status, request.getRequestURI(), ErrorCode.GMAIL_API_ERROR);
    }

    @ExceptionHandler(TokenCreationException.class)
    public ResponseEntity<ErrorResponse> handleTokenCreationException(TokenCreationException ex, HttpServletRequest request) {
        log.warn("Failed to create reset token after multiple attempts.", ex);
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
            log.warn("Failed to upload file. File size exceeds the maximum allowed size of 10MB.");
            return buildErrorResponse(
                    "File size exceeds the maximum allowed size of 10MB.",
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    request.getRequestURI(),
                    ErrorCode.FILE_UPLOAD_FAILED
            );
        }
        log.warn("Multipart error: ", ex);
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

    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaTypeStatusException(UnsupportedMediaTypeStatusException ex, HttpServletRequest request) {
        log.warn("Unsupported media format. ", ex);
        return buildErrorResponse(ex.getMessage(), HttpStatus.UNSUPPORTED_MEDIA_TYPE, request.getRequestURI(), ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(InvalidProfileDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProfileData(
            InvalidProfileDataException ex, HttpServletRequest request) {
        log.warn("Invalid profile data: {}", ex.getMessage());
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.VALIDATION_FAILED
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Database constraint violation", ex);
        String rawMessage = ex.getMostSpecificCause().getMessage();
        String userMessage = "A database constraint was violated. Please check related entities or required fields.";

        if (rawMessage != null) {
            if (rawMessage.contains("username")) {
                userMessage = "Username already exists.";
            } else if (rawMessage.contains("email")) {
                userMessage = "Email address already exists.";
            } else if (rawMessage.contains("contract_id")) {
                userMessage = "Milestone must be linked to a contract before saving.";
            }
        }

        return buildErrorResponse(
                userMessage,
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                ErrorCode.VALIDATION_FAILED
        );
    }

    @ExceptionHandler(ProjectAccessException.class)
    public ResponseEntity<ErrorResponse> handleProjectAccess(ProjectAccessException ex,
                                                             HttpServletRequest request) {
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.FORBIDDEN,
                request.getRequestURI(),
                ErrorCode.ACCESS_DENIED
        );
    }

    @ExceptionHandler(RoleAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleRoleAccess(RoleAccessDeniedException ex,
                                                          HttpServletRequest request) {
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.FORBIDDEN,
                request.getRequestURI(),
                ErrorCode.ACCESS_DENIED
        );
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorResponse> handleDateTimeParse(DateTimeParseException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        return buildErrorResponse(
                "Invalid date format. Please use yyyy-MM-dd.",
                HttpStatus.BAD_REQUEST,
                path,
                ErrorCode.INVALID_DATE_FORMAT
        );
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(Object message, HttpStatus status, String path, ErrorCode errorCode) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message, path, errorCode));
    }
}
