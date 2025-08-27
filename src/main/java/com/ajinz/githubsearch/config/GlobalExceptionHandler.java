package com.ajinz.githubsearch.config;

import com.ajinz.githubsearch.dto.github.ApiErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex, WebRequest request) {

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

    // Convert validation errors to a readable message
    String errorMessage =
        "Validation failed: "
            + errors.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue())
                .collect(Collectors.joining(", "));

    ApiErrorResponse errorResponse =
        new ApiErrorResponse(
            errorMessage,
            "VALIDATION_ERROR",
            HttpStatus.BAD_REQUEST.value(),
            request.getDescription(false).replace("uri=", ""));

    logger.warn("Validation error: {}", errors);
    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiErrorResponse> handleRuntimeException(
      RuntimeException ex, WebRequest request) {

    ApiErrorResponse errorResponse =
        new ApiErrorResponse(
            ex.getMessage(),
            "INTERNAL_ERROR",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getDescription(false).replace("uri=", ""));

    logger.error("Runtime exception occurred", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleJsonParseException(
      HttpMessageNotReadableException ex, HttpServletRequest request) {

    String message = "Invalid request format";

    // Extract more specific error for enum parsing
    if (ex.getCause() instanceof InvalidFormatException invalidFormatEx) {
      if (invalidFormatEx.getTargetType().isEnum()) {
        Object[] enumValues = invalidFormatEx.getTargetType().getEnumConstants();
        String validValues =
            Arrays.stream(enumValues).map(Object::toString).collect(Collectors.joining(", "));

        message =
            String.format(
                "Invalid value '%s' for %s. Valid values are: [%s]",
                invalidFormatEx.getValue(),
                invalidFormatEx.getTargetType().getSimpleName(),
                validValues);
      }
    }

    ApiErrorResponse errorResponse =
        new ApiErrorResponse(message, "BAD_REQUEST", 400, request.getRequestURI());

    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, WebRequest request) {

    ApiErrorResponse errorResponse =
        new ApiErrorResponse(
            "An unexpected error occurred",
            "UNEXPECTED_ERROR",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getDescription(false).replace("uri=", ""));

    logger.error("Unexpected exception occurred", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }
}
