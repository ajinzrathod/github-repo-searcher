package com.ajinz.githubsearch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ajinz.githubsearch.dto.github.GitHubSearchResponse;
import com.ajinz.githubsearch.dto.github.GithubSearchRequest;
import com.ajinz.githubsearch.dto.github.Order;
import com.ajinz.githubsearch.dto.github.Sort;
import com.ajinz.githubsearch.service.GitHubSearchService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

class SearchControllerUnitTest {

  @Mock private GitHubSearchService gitHubSearchService;

  private SearchController searchController;

  @BeforeEach
  void setUp() {
    try (var mocks = MockitoAnnotations.openMocks(this)) {
      searchController = new SearchController(gitHubSearchService);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void shouldReturnSuccessfulResponseForPostRequest() {
    // Given
    GitHubSearchResponse mockResponse = new GitHubSearchResponse(2, false, Collections.emptyList());
    GithubSearchRequest searchRequest =
        new GithubSearchRequest("react", "javascript", Sort.FORKS, Order.DESC, 1, 5);

    when(gitHubSearchService.searchRepositories(any(GithubSearchRequest.class)))
        .thenReturn(Mono.just(mockResponse));

    // When
    Mono<ResponseEntity<GitHubSearchResponse>> result =
        searchController.searchRepositories(searchRequest);

    // Then
    ResponseEntity<GitHubSearchResponse> response = result.block();
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(mockResponse, response.getBody());
    verify(gitHubSearchService).searchRepositories(any(GithubSearchRequest.class));
  }

  @Test
  void shouldHandleServiceErrorForPostRequest() {
    // Given
    GithubSearchRequest searchRequest =
        new GithubSearchRequest("react", null, Sort.FORKS, Order.DESC, 1, 10);

    when(gitHubSearchService.searchRepositories(any(GithubSearchRequest.class)))
        .thenReturn(Mono.error(new RuntimeException("Service error")));

    // When
    Mono<ResponseEntity<GitHubSearchResponse>> result =
        searchController.searchRepositories(searchRequest);

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
  void shouldPassCorrectSearchRequestToService() {
    // Given
    GitHubSearchResponse mockResponse = new GitHubSearchResponse(1, false, Collections.emptyList());
    GithubSearchRequest searchRequest =
        new GithubSearchRequest("spring boot", "java", Sort.FORKS, Order.DESC, 2, 20);

    when(gitHubSearchService.searchRepositories(any(GithubSearchRequest.class)))
        .thenReturn(Mono.just(mockResponse));

    // When
    searchController.searchRepositories(searchRequest).block();

    // Then - Verify the SearchRequest was passed correctly
    verify(gitHubSearchService).searchRepositories(searchRequest);
  }
}
