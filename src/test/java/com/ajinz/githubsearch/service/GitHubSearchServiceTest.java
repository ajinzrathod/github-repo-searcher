package com.ajinz.githubsearch.service;

import static org.junit.jupiter.api.Assertions.*;

import com.ajinz.githubsearch.dto.github.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
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

class GitHubSearchServiceTest {

  private MockWebServer mockWebServer;
  private GitHubSearchService gitHubSearchService;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

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
    GithubSearchRequest request =
        new GithubSearchRequest("spring", "java", Sort.STARS, Order.DESC, 1, 10);

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
                    && response.items().getFirst().name().equals("spring-boot"))
        .verifyComplete();

    // Verify the request was made correctly
    RecordedRequest recordedRequest = mockWebServer.takeRequest();
    assertEquals("GET", recordedRequest.getMethod());
    assertNotNull(recordedRequest.getPath());
    assertTrue(recordedRequest.getPath().contains("/search/repositories"));
    assertTrue(recordedRequest.getPath().contains("q=spring%20language:java"));
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

    GitHubRepository repository1 =
        new GitHubRepository(
            6296790L,
            "spring-boot",
            "spring-projects/spring-boot",
            "Spring Boot",
            "https://github.com/spring-projects/spring-boot",
            "https://spring.io/projects/spring-boot",
            "Java",
            65000,
            35000,
            65000,
            50000,
            LocalDateTime.of(2012, 10, 19, 15, 21, 38),
            LocalDateTime.of(2023, 8, 24, 10, 30, 0),
            LocalDateTime.of(2023, 8, 24, 10, 0, 0),
            owner1);

    List<GitHubRepository> repositories = getGitHubRepositories(owner2, repository1);

    return new GitHubSearchResponse(2, false, repositories);
  }

  @NotNull
  private static List<GitHubRepository> getGitHubRepositories(
      GitHubOwner owner2, GitHubRepository repository1) {
    GitHubRepository repository2 =
        new GitHubRepository(
            1234567L,
            "spring-framework",
            "spring-projects/spring-framework",
            "Spring Framework",
            "https://github.com/spring-projects/spring-framework",
            "https://spring.io/projects/spring-framework",
            "Java",
            45000,
            25000,
            45000,
            30000,
            LocalDateTime.of(2008, 1, 1, 10, 0, 0),
            LocalDateTime.of(2023, 8, 23, 14, 15, 30),
            LocalDateTime.of(2023, 8, 23, 14, 0, 0),
            owner2);

    return Arrays.asList(repository1, repository2);
  }

  @Test
  void shouldBuildQueryWithLanguageFilter() throws JsonProcessingException, InterruptedException {
    // Given
    GithubSearchRequest request =
        new GithubSearchRequest("react", "javascript", Sort.FORKS, Order.DESC, 1, 5);
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
    GithubSearchRequest request =
        new GithubSearchRequest("nodejs", null, Sort.FORKS, Order.DESC, 1, 10);
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
    assertTrue(recordedRequest.getPath().contains("q=nodejs"));
    assertFalse(recordedRequest.getPath().contains("language:"));
  }

  @Test
  void shouldHandleGitHubApiError() {
    // Given
    GithubSearchRequest request =
        new GithubSearchRequest("test", null, Sort.FORKS, Order.DESC, 1, 10);

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
                    && throwable
                        .getMessage()
                        .contains("An unexpected error occurred while searching repositories"))
        .verify();
  }

  @Test
  void shouldSetCorrectHeaders() throws InterruptedException, JsonProcessingException {
    // Given
    GithubSearchRequest request =
        new GithubSearchRequest("test", null, Sort.FORKS, Order.DESC, 1, 10);
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
    GithubSearchRequest request =
        new GithubSearchRequest("python", "", Sort.FORKS, Order.DESC, 1, 10);
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
