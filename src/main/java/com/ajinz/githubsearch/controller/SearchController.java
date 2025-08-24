package com.ajinz.githubsearch.controller;

import com.ajinz.githubsearch.dto.GitHubSearchResponse;
import com.ajinz.githubsearch.dto.SearchRequest;
import com.ajinz.githubsearch.service.GitHubSearchService;
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

  @GetMapping("/search/repositories")
  public Mono<ResponseEntity<GitHubSearchResponse>> searchRepositories(
      @RequestParam String query,
      @RequestParam(required = false) String language,
      @RequestParam(defaultValue = "stars") String sort,
      @RequestParam(defaultValue = "desc") String order,
      @RequestParam(defaultValue = "1") Integer page,
      @RequestParam(defaultValue = "10", name = "per_page") Integer perPage) {

    SearchRequest searchRequest = new SearchRequest(query, language, sort, order, page, perPage);
    logger.info("Received GET search request for query: {}", query);

    return gitHubSearchService
        .searchRepositories(searchRequest)
        .map(ResponseEntity::ok)
        .onErrorReturn(ResponseEntity.internalServerError().build());
  }
}
