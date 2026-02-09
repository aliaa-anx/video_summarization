package com.backend.text_summarizer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // This catches the specific RuntimeException you throw (e.g., "Email already taken")
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        // 1. Create a clean JSON object
        Map<String, String> errorResponse = new HashMap<>();

        // 2. Put the error message inside a "message" key
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());

        // 3. Return HTTP 400 (Bad Request) so the frontend knows it failed
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}