package com.ajinz.githubsearch.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiErrorResponse(
    String message,
    String error,
    int status,
    LocalDateTime timestamp,
    String path,
    Map<String, String> validationErrors
) {
    public ApiErrorResponse(String message, String error, int status, String path) {
        this(message, error, status, LocalDateTime.now(), path, null);
    }

    public ApiErrorResponse(String message, String error, int status, String path, Map<String, String> validationErrors) {
        this(message, error, status, LocalDateTime.now(), path, validationErrors);
    }
}
