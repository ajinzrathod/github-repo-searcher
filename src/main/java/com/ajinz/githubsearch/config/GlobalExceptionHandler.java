package com.ajinz.githubsearch.config;

import com.ajinz.githubsearch.dto.github.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationErrors(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    logger.error("Validation error occurred", ex);

    String message = "Validation failed";
    if (ex.getBindingResult().hasFieldErrors()) {
      message = ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
    }

    ApiErrorResponse errorResponse =
        new ApiErrorResponse(
            message,
            "VALIDATION_ERROR",
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            request.getRequestURI());

    return ResponseEntity.badRequest().body(errorResponse);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiErrorResponse> handleRuntimeException(
      RuntimeException ex, HttpServletRequest request) {
    logger.error("Runtime exception occurred", ex);

    ApiErrorResponse errorResponse =
        new ApiErrorResponse(
            "An unexpected error occurred",
            "UNEXPECTED_ERROR",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            LocalDateTime.now(),
            request.getRequestURI());

    return ResponseEntity.internalServerError().body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGenericException(
      Exception ex, HttpServletRequest request) {
    logger.error("Unexpected exception occurred", ex);

    ApiErrorResponse errorResponse =
        new ApiErrorResponse(
            "An unexpected error occurred",
            "UNEXPECTED_ERROR",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            LocalDateTime.now(),
            request.getRequestURI());

    return ResponseEntity.internalServerError().body(errorResponse);
  }
}
