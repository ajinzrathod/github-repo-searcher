package com.ajinz.githubsearch.dto.github;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record GithubSearchRequest(
    @NotBlank(message = "Query cannot be blank") String query,
    String language,
    Sort sort,
    Order order,
    @Min(value = 1, message = "Page must be at least 1")
        @Max(value = 34, message = "Page cannot exceed 34 (GitHub API limit)")
        Integer page,
    @Min(value = 1, message = "Per page must be at least 1")
        @Max(value = 100, message = "Per page cannot exceed 100")
        Integer perPage) {
  public GithubSearchRequest {
    // Set defaults
    page = page != null ? page : 1;
    perPage = perPage != null ? perPage : 10;
    sort = sort != null ? sort : Sort.STARS;
    order = order != null ? order : Order.DESC;
  }
}
