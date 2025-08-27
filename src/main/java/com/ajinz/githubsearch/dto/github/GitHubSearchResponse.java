package com.ajinz.githubsearch.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GitHubSearchResponse(
    @JsonProperty("total_count") Integer totalCount,
    @JsonProperty("incomplete_results") Boolean incompleteResults,
    List<GitHubRepository> items) {}
