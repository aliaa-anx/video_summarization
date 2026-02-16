package com.backend_microservices.notification_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        // 1. Create the clean JSON map
        Map<String, String> errorResponse = new HashMap<>();

        // 2. Add the error details
        errorResponse.put("error", "Email Sending Failed");
        errorResponse.put("message", ex.getMessage()); // This will contain "Error sending email..."

        // 3. Return HTTP 500 (Internal Server Error)
        // We use 500 here because if email fails, it's usually a system/server issue, not the user's input.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
