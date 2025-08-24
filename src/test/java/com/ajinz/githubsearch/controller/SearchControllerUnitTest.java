package com.ajinz.githubsearch.controller;

import com.ajinz.githubsearch.dto.GitHubSearchResponse;
import com.ajinz.githubsearch.dto.SearchRequest;
import com.ajinz.githubsearch.service.GitHubSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchControllerUnitTest {

  @Mock private GitHubSearchService gitHubSearchService;

  private SearchController searchController;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    searchController = new SearchController(gitHubSearchService);
  }

  @Test
  void shouldReturnSuccessfulResponseForGetRequest() {
    // Given
    GitHubSearchResponse mockResponse = new GitHubSearchResponse(
        2, false, Collections.emptyList()
    );

    when(gitHubSearchService.searchRepositories(any(SearchRequest.class)))
        .thenReturn(Mono.just(mockResponse));

    // When
    Mono<ResponseEntity<GitHubSearchResponse>> result =
        searchController.searchRepositories("react", "javascript", "stars", "desc", 1, 5);

    // Then
    ResponseEntity<GitHubSearchResponse> response = result.block();
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(mockResponse, response.getBody());
    verify(gitHubSearchService).searchRepositories(any(SearchRequest.class));
  }

  @Test
  void shouldHandleServiceErrorForGetRequest() {
    // Given
    when(gitHubSearchService.searchRepositories(any(SearchRequest.class)))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    // When
    Mono<ResponseEntity<GitHubSearchResponse>> result =
        searchController.searchRepositories("react", null, "stars", "desc", 1, 10);

    // Then
    ResponseEntity<GitHubSearchResponse> response = result.block();
    assertNotNull(response);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
  }

  @Test
  void shouldReturnHealthStatus() {
    // When
    ResponseEntity<String> response = searchController.health();

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("OK", response.getBody());
  }

  @Test
  void shouldCreateCorrectSearchRequestFromGetParameters() {
    // Given
    GitHubSearchResponse mockResponse = new GitHubSearchResponse(
        1, false, Collections.emptyList()
    );

    when(gitHubSearchService.searchRepositories(any(SearchRequest.class)))
        .thenReturn(Mono.just(mockResponse));

    // When
    searchController.searchRepositories("spring boot", "java", "forks", "asc", 2, 20).block();

    // Then - Verify the SearchRequest was created correctly
    verify(gitHubSearchService).searchRepositories(
        new SearchRequest("spring boot", "java", "forks", "asc", 2, 20)
    );
  }

  @Test
  void shouldHandleNullLanguageParameter() {
    // Given
    GitHubSearchResponse mockResponse = new GitHubSearchResponse(
        1, false, Collections.emptyList()
    );

    when(gitHubSearchService.searchRepositories(any(SearchRequest.class)))
        .thenReturn(Mono.just(mockResponse));

    // When
    Mono<ResponseEntity<GitHubSearchResponse>> result =
        searchController.searchRepositories("python", null, "stars", "desc", 1, 10);

    // Then
    ResponseEntity<GitHubSearchResponse> response = result.block();
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Verify SearchRequest was created with null language
    verify(gitHubSearchService).searchRepositories(
        new SearchRequest("python", null, "stars", "desc", 1, 10)
    );
  }

  @Test
  void shouldUseDefaultParameters() {
    // Given
    GitHubSearchResponse mockResponse = new GitHubSearchResponse(
        1, false, Collections.emptyList()
    );

    when(gitHubSearchService.searchRepositories(any(SearchRequest.class)))
        .thenReturn(Mono.just(mockResponse));

    // When - Only provide required query parameter
    searchController.searchRepositories("test", null, "stars", "desc", 1, 10).block();

    // Then - Verify defaults are used
    verify(gitHubSearchService).searchRepositories(
        new SearchRequest("test", null, "stars", "desc", 1, 10)
    );
  }
}
