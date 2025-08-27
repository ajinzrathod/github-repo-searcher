package com.ajinz.githubsearch.controller;

import com.ajinz.githubsearch.dto.github.GitHubSearchResponse;
import com.ajinz.githubsearch.dto.github.GithubSearchRequest;
import com.ajinz.githubsearch.service.GitHubSearchService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RequestMapping("/api/github")
@RestController
public class SearchController {

  private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

  private final GitHubSearchService gitHubSearchService;

  public SearchController(GitHubSearchService gitHubSearchService) {
    this.gitHubSearchService = gitHubSearchService;
  }

  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("OK");
  }

  @PostMapping("/search/repositories")
  public Mono<ResponseEntity<GitHubSearchResponse>> searchRepositories(
      @Valid @RequestBody GithubSearchRequest githubSearchRequest) {

    logger.info("Received POST search request for query: {}", githubSearchRequest.query());

    return gitHubSearchService
        .searchRepositories(githubSearchRequest)
        .map(ResponseEntity::ok)
        .onErrorReturn(ResponseEntity.internalServerError().build());
  }
}
