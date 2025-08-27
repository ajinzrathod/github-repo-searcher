package com.ajinz.githubsearch.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record GitHubRepository(
    Long id,
    String name,
    @JsonProperty("full_name") String fullName,
    String description,
    @JsonProperty("html_url") String url,
    String homepage,
    String language,
    @JsonProperty("stargazers_count") Integer stargazersCount,
    @JsonProperty("forks_count") Integer forksCount,
    @JsonProperty("watchers_count") Integer watchersCount,
    Integer size,
    @JsonProperty("created_at") LocalDateTime createdAt,
    @JsonProperty("updated_at") LocalDateTime updatedAt,
    @JsonProperty("pushed_at") LocalDateTime pushedAt,
    GitHubOwner owner) {}
