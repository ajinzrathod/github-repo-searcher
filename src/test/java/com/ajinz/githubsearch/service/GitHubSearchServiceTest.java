package com.ajinz.githubsearch.service;

import com.ajinz.githubsearch.dto.GitHubSearchResponse;
import com.ajinz.githubsearch.dto.GitHubRepository;
import com.ajinz.githubsearch.dto.SearchRequest;
import com.ajinz.githubsearch.dto.GitHubOwner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitHubSearchServiceTest {

  private MockWebServer mockWebServer;
  private GitHubSearchService gitHubSearchService;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    objectMapper = new ObjectMapper();

    String baseUrl = mockWebServer.url("/").toString();
    WebClient.Builder webClientBuilder = WebClient.builder();

    gitHubSearchService = new GitHubSearchService(webClientBuilder, baseUrl, "2022-11-28");
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void shouldSearchRepositoriesSuccessfully() throws JsonProcessingException, InterruptedException {
    // Given
    SearchRequest request = new SearchRequest("spring", "java", "stars", "desc", 1, 10);

    GitHubSearchResponse mockResponse = getGitHubSearchResponse();

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(mockResponse))
            .addHeader("Content-Type", "application/json"));

    // When
    Mono<GitHubSearchResponse> result = gitHubSearchService.searchRepositories(request);

    // Then
    StepVerifier.create(result)
        .expectNextMatches(
            response ->
                response.totalCount() == 2
                    && response.items().size() == 2
                    && response.items().get(0).name().equals("spring-boot"))
        .verifyComplete();

    // Verify the request was made correctly
    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("GET", recordedRequest.getMethod());
    assertTrue(recordedRequest.getPath().contains("/search/repositories"));
    assertTrue(recordedRequest.getPath().contains("q=spring%20language:java"));
    assertTrue(recordedRequest.getPath().contains("sort=stars"));
    assertTrue(recordedRequest.getPath().contains("order=desc"));
    assertTrue(recordedRequest.getPath().contains("per_page=10"));
    assertTrue(recordedRequest.getPath().contains("page=1"));
  }

  @NotNull
  private static GitHubSearchResponse getGitHubSearchResponse() {
    GitHubOwner owner1 =
        new GitHubOwner(
            1L,
            "spring-projects",
            "https://avatars.githubusercontent.com/u/317776",
            "https://github.com/spring-projects",
            "Organization");
    GitHubOwner owner2 =
        new GitHubOwner(
            2L,
            "spring-projects",
            "https://avatars.githubusercontent.com/u/317776",
            "https://github.com/spring-projects",
            "Organization");

    List<GitHubRepository> repositories =
        Arrays.asList(
            new GitHubRepository(
                1L,
                "spring-boot",
                "spring-projects/spring-boot",
                "Spring Boot helps you to create Spring-powered, production-grade applications",
                "https://github.com/spring-projects/spring-boot",
                "https://github.com/spring-projects/spring-boot.git",
                "Java",
                65000,
                60000,
                40000,
                500,
                LocalDateTime.of(2013, 12, 10, 0, 0),
                LocalDateTime.of(2024, 1, 15, 0, 0),
                LocalDateTime.of(2024, 1, 15, 0, 0),
                owner1),
            new GitHubRepository(
                2L,
                "spring-framework",
                "spring-projects/spring-framework",
                "Spring Framework core",
                "https://github.com/spring-projects/spring-framework",
                "https://github.com/spring-projects/spring-framework.git",
                "Java",
                50000,
                45000,
                30000,
                300,
                LocalDateTime.of(2008, 12, 21, 0, 0),
                LocalDateTime.of(2024, 1, 14, 0, 0),
                LocalDateTime.of(2024, 1, 14, 0, 0),
                owner2));

    return new GitHubSearchResponse(2, false, repositories);
  }

  @Test
  void shouldBuildQueryWithLanguageFilter() throws JsonProcessingException, InterruptedException {
    // Given
    SearchRequest request = new SearchRequest("react", "javascript", "stars", "desc", 1, 5);
    GitHubSearchResponse mockResponse = new GitHubSearchResponse(0, false, List.of());

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(mockResponse))
            .addHeader("Content-Type", "application/json"));

    // When
    gitHubSearchService.searchRepositories(request).block();

    // Then
    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertNotNull(recordedRequest.getPath());
    assertTrue(recordedRequest.getPath().contains("q=react%20language:javascript"));
  }

  @Test
  void shouldBuildQueryWithoutLanguageFilter()
      throws JsonProcessingException, InterruptedException {
    // Given
    SearchRequest request = new SearchRequest("nodejs", null, "stars", "desc", 1, 10);
    GitHubSearchResponse mockResponse = new GitHubSearchResponse(0, false, Arrays.asList());

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(mockResponse))
            .addHeader("Content-Type", "application/json"));

    // When
    gitHubSearchService.searchRepositories(request).block();

    // Then
    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertNotNull(recordedRequest.getPath());
    assertTrue(recordedRequest.getPath().contains("q=nodejs"));
    assertFalse(recordedRequest.getPath().contains("language:"));
  }

  @Test
  void shouldHandleGitHubApiError() {
    // Given
    SearchRequest request = new SearchRequest("test", null, "stars", "desc", 1, 10);

    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(403)
            .setBody("{\"message\":\"API rate limit exceeded\"}")
            .addHeader("Content-Type", "application/json"));

    // When
    Mono<GitHubSearchResponse> result = gitHubSearchService.searchRepositories(request);

    // Then
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().contains("Failed to search repositories"))
        .verify();
  }

  @Test
  void shouldSetCorrectHeaders() throws InterruptedException, JsonProcessingException {
    // Given
    SearchRequest request = new SearchRequest("test", null, "stars", "desc", 1, 10);
    GitHubSearchResponse mockResponse = new GitHubSearchResponse(0, false, List.of());

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(mockResponse))
            .addHeader("Content-Type", "application/json"));

    // When
    gitHubSearchService.searchRepositories(request).block();

    // Then
    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("application/vnd.github+json", recordedRequest.getHeader("Accept"));
    assertEquals("2022-11-28", recordedRequest.getHeader("X-GitHub-Api-Version"));
  }

  @Test
  void shouldHandleEmptyLanguage() throws JsonProcessingException, InterruptedException {
    // Given
    SearchRequest request = new SearchRequest("python", "", "stars", "desc", 1, 10);
    GitHubSearchResponse mockResponse = new GitHubSearchResponse(0, false, List.of());

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(mockResponse))
            .addHeader("Content-Type", "application/json"));

    // When
    gitHubSearchService.searchRepositories(request).block();

    // Then
    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertNotNull(recordedRequest.getPath());
    assertTrue(recordedRequest.getPath().contains("q=python"));
    assertFalse(recordedRequest.getPath().contains("language:"));
  }
}
