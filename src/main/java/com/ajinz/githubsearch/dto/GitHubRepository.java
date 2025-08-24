package com.ajinz.githubsearch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record GitHubRepository(
    Long id,
    String name,

    @JsonProperty("full_name")
    String fullName,

    String description,

    @JsonProperty("html_url")
    String htmlUrl,

    @JsonProperty("clone_url")
    String cloneUrl,

    String language,

    @JsonProperty("stargazers_count")
    Integer stargazersCount,

    @JsonProperty("watchers_count")
    Integer watchersCount,

    @JsonProperty("forks_count")
    Integer forksCount,

    @JsonProperty("open_issues_count")
    Integer openIssuesCount,

    @JsonProperty("created_at")
    LocalDateTime createdAt,

    @JsonProperty("updated_at")
    LocalDateTime updatedAt,

    @JsonProperty("pushed_at")
    LocalDateTime pushedAt,

    GitHubOwner owner
) {}
