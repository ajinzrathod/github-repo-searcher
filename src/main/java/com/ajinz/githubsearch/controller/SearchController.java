package com.ajinz.githubsearch.controller;

import com.ajinz.githubsearch.dto.github.GitHubRepository;
import com.ajinz.githubsearch.dto.github.GitHubSearchResponse;
import com.ajinz.githubsearch.dto.github.GithubSearchRequest;
import com.ajinz.githubsearch.service.GitHubRepositoryService;
import com.ajinz.githubsearch.service.GitHubSearchService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/github")
@RestController
public class SearchController {

  private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

  private final GitHubRepositoryService gitHubRepositoryService;
  private final GitHubSearchService gitHubSearchService;

  public SearchController(
      GitHubRepositoryService gitHubRepositoryService, GitHubSearchService gitHubSearchService) {
    this.gitHubRepositoryService = gitHubRepositoryService;
    this.gitHubSearchService = gitHubSearchService;
  }

  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("OK");
  }

  @PostMapping("/search/repositories")
  public Callable<ResponseEntity<GitHubSearchResponse>> searchRepositories(
      @Valid @RequestBody GithubSearchRequest request) {
    logger.info("Received POST request to search repositories with query: {}", request.query());

    return () -> {
      try {
        GitHubSearchResponse response = gitHubSearchService.searchRepositories(request).block();
        logger.info("Successfully completed async search for query: {}", request.query());
        return ResponseEntity.ok(response);
      } catch (Exception e) {
        logger.error("Error during async repository search", e);
        throw e;
      }
    };
  }

  @GetMapping("/repositories")
  public ResponseEntity<List<GitHubRepository>> getFilteredRepositories(
      @RequestParam(required = false) String language,
      @RequestParam(required = false) Integer minStars,
      @RequestParam(required = false, defaultValue = "stars") String sort) {

    logger.info(
        "Received GET request for repositories with filters - language: {}, minStars: {}, sort: {}",
        language,
        minStars,
        sort);

    try {
      List<GitHubRepository> repositories =
          gitHubRepositoryService.getFilteredRepositories(language, minStars, sort);

      logger.info("Returning {} filtered repositories", repositories.size());
      return ResponseEntity.ok(repositories);
    } catch (Exception e) {
      logger.error("Error fetching filtered repositories", e);
      return ResponseEntity.internalServerError().build();
    }
  }
}
