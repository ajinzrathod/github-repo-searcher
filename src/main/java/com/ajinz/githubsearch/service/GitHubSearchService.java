package com.ajinz.githubsearch.service;

import com.ajinz.githubsearch.dto.github.GitHubSearchResponse;
import com.ajinz.githubsearch.dto.github.GithubSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class GitHubSearchService {

  private static final Logger logger = LoggerFactory.getLogger(GitHubSearchService.class);

  private final WebClient webClient;

  public GitHubSearchService(
      WebClient.Builder webClientBuilder,
      @Value("${github.api.base-url:https://api.github.com}") String baseUrl,
      @Value("${github.api.version:2022-11-28}") String apiVersion) {
    this.webClient =
        webClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", apiVersion)
            .build();
  }

  public Mono<GitHubSearchResponse> searchRepositories(GithubSearchRequest githubSearchRequest) {
    logger.info("Searching repositories with query: {}", githubSearchRequest.query());
    String query = buildQuery(githubSearchRequest.query(), githubSearchRequest.language());

    return webClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/search/repositories")
                    .queryParam("q", query)
                    .queryParam("sort", githubSearchRequest.sort())
                    .queryParam("order", githubSearchRequest.order())
                    .queryParam("per_page", githubSearchRequest.perPage())
                    .queryParam("page", githubSearchRequest.page())
                    .build())
        .retrieve()
        .bodyToMono(GitHubSearchResponse.class)
        .doOnSuccess(
            response ->
                logger.info(
                    "Successfully retrieved {} repositories",
                    response != null ? response.items().size() : 0))
        .onErrorMap(
            WebClientResponseException.class,
            ex -> {
              logger.error(
                  "GitHub API error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
              return new RuntimeException("Failed to search repositories: " + ex.getMessage());
            })
        .onErrorMap(
            Exception.class,
            ex -> {
              logger.error("Unexpected error during repository search", ex);
              return new RuntimeException(
                  "An unexpected error occurred while searching repositories");
            });
  }

  private String buildQuery(String query, String language) {
    if (language != null && !language.trim().isEmpty()) {
      return query + " language:" + language;
    }
    return query;
  }
}
