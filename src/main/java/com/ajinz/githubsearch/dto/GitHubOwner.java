package com.ajinz.githubsearch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubOwner(
    Long id,
    String login,

    @JsonProperty("avatar_url")
    String avatarUrl,

    @JsonProperty("html_url")
    String htmlUrl,

    String type
) {}
