package com.ajinz.githubsearch.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
    @NotBlank(message = "Query cannot be blank")
    String query,

    String language,

    String sort, // stars, forks, updated

    String order, // asc, desc

    @Min(value = 1, message = "Page must be at least 1")
    @Max(value = 34, message = "Page cannot exceed 34 (GitHub API limit)")
    Integer page,

    @Min(value = 1, message = "Per page must be at least 1")
    @Max(value = 100, message = "Per page cannot exceed 100")
    Integer perPage
) {
    public SearchRequest {
        // Set defaults
        page = page != null ? page : 1;
        perPage = perPage != null ? perPage : 10;
        sort = sort != null ? sort : "stars";
        order = order != null ? order : "desc";
    }
}
