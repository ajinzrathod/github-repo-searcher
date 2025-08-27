package com.ajinz.githubsearch.dto.github;

import java.time.LocalDateTime;

public record ApiErrorResponse(
    String message, String error, int status, LocalDateTime timestamp, String path) {

  public ApiErrorResponse(String message, String error, int status, String path) {
    this(message, error, status, LocalDateTime.now(), path);
  }
}
