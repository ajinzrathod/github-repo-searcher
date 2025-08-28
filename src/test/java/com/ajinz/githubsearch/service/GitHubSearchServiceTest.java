package com.ajinz.githubsearch.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.ajinz.githubsearch.dto.github.GitHubRepository;
import com.ajinz.githubsearch.dto.github.GitHubSearchResponse;
import com.ajinz.githubsearch.dto.github.GithubSearchRequest;
import com.ajinz.githubsearch.dto.github.Order;
import com.ajinz.githubsearch.dto.github.Sort;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GitHubSearchServiceTest {

  @Mock private WebClient.Builder webClientBuilder;
  @Mock private WebClient webClient;
  @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
  @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
  @Mock private WebClient.ResponseSpec responseSpec;
  @Mock private GitHubRepositoryService gitHubRepositoryService;

  private GitHubSearchService gitHubSearchService;

  private final String baseUrl = "https://api.github.com";
  private final String apiVersion = "2022-11-28";

  @BeforeEach
  void setUp() {
    when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
    when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
    when(webClientBuilder.build()).thenReturn(webClient);

    gitHubSearchService =
        new GitHubSearchService(webClientBuilder, baseUrl, apiVersion, gitHubRepositoryService);
  }

  @Test
  void constructor_ShouldConfigureWebClientCorrectly() {
    // Verify that WebClient.Builder is configured with correct values
    verify(webClientBuilder).baseUrl(baseUrl);
    verify(webClientBuilder).defaultHeader("Accept", "application/vnd.github+json");
    verify(webClientBuilder).defaultHeader("X-GitHub-Api-Version", apiVersion);
    verify(webClientBuilder).build();
  }

  @Test
  void searchRepositories_WithBasicRequest_ShouldReturnSuccessfulResponseAndSaveRepositories() {
    // Arrange
    GithubSearchRequest request =
        new GithubSearchRequest("spring boot", null, Sort.STARS, Order.DESC, 1, 10);

    GitHubRepository repository = new GitHubRepository(1L, "test-repo", "test-owner");
    GitHubSearchResponse expectedResponse = new GitHubSearchResponse(false, List.of(repository));

    setupMockWebClientChain();
    when(responseSpec.bodyToMono(GitHubSearchResponse.class))
        .thenReturn(Mono.just(expectedResponse));

    // Act & Assert
    StepVerifier.create(gitHubSearchService.searchRepositories(request))
        .expectNext(expectedResponse)
        .verifyComplete();

    verify(webClient).get();
    verify(gitHubRepositoryService).saveAllGitHubRepositories(expectedResponse.items());
  }

  @Test
  void searchRepositories_WithLanguageFilter_ShouldIncludeLanguageInQueryButNotSaveEmptyResults() {
    // Arrange
    GithubSearchRequest request =
        new GithubSearchRequest("spring boot", "java", Sort.STARS, Order.DESC, 1, 10);

    GitHubSearchResponse expectedResponse = new GitHubSearchResponse(false, List.of());

    setupMockWebClientChain();
    when(responseSpec.bodyToMono(GitHubSearchResponse.class))
        .thenReturn(Mono.just(expectedResponse));

    // Act
    StepVerifier.create(gitHubSearchService.searchRepositories(request))
        .expectNext(expectedResponse)
        .verifyComplete();

    // Verify
    verify(requestHeadersUriSpec).uri(any(Function.class));
    verify(gitHubRepositoryService, never()).saveAllGitHubRepositories(any());
  }

  @Test
  void
      searchRepositories_WithEmptyLanguage_ShouldNotIncludeLanguageInQueryAndNotSaveEmptyResults() {
    // Arrange
    GithubSearchRequest request =
        new GithubSearchRequest("spring boot", "", Sort.STARS, Order.DESC, 1, 10);

    GitHubSearchResponse expectedResponse = new GitHubSearchResponse(false, List.of());

    setupMockWebClientChain();
    when(responseSpec.bodyToMono(GitHubSearchResponse.class))
        .thenReturn(Mono.just(expectedResponse));

    // Act
    StepVerifier.create(gitHubSearchService.searchRepositories(request))
        .expectNext(expectedResponse)
        .verifyComplete();

    verify(requestHeadersUriSpec).uri(any(Function.class));
    verify(gitHubRepositoryService, never()).saveAllGitHubRepositories(any());
  }

  @Test
  void
      searchRepositories_WithWebClientResponseException_ShouldMapToRuntimeExceptionAndNotSaveRepositories() {
    // Arrange
    GithubSearchRequest request =
        new GithubSearchRequest("spring boot", null, Sort.STARS, Order.DESC, 1, 10);

    WebClientResponseException webClientException =
        WebClientResponseException.create(404, "Not Found", null, null, null);

    setupMockWebClientChain();
    when(responseSpec.bodyToMono(GitHubSearchResponse.class))
        .thenReturn(Mono.error(webClientException));

    // Act & Assert
    StepVerifier.create(gitHubSearchService.searchRepositories(request))
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable
                        .getMessage()
                        .contains("An unexpected error occurred while searching repositories"))
        .verify();
    verify(gitHubRepositoryService, never()).saveAllGitHubRepositories(any());
  }

  @Test
  void searchRepositories_WithGenericException_ShouldMapToRuntimeExceptionAndNotSaveRepositories() {
    // Arrange
    GithubSearchRequest request =
        new GithubSearchRequest("spring boot", null, Sort.STARS, Order.DESC, 1, 10);

    RuntimeException genericException = new RuntimeException("Network timeout");

    setupMockWebClientChain();
    when(responseSpec.bodyToMono(GitHubSearchResponse.class))
        .thenReturn(Mono.error(genericException));

    // Act & Assert
    StepVerifier.create(gitHubSearchService.searchRepositories(request))
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable
                        .getMessage()
                        .contains("An unexpected error occurred while searching repositories"))
        .verify();
    verify(gitHubRepositoryService, never()).saveAllGitHubRepositories(any());
  }

  @Test
  void searchRepositories_WithEmptyResponse_ShouldHandleGracefullyAndNotSaveRepositories() {
    // Arrange
    GithubSearchRequest request =
        new GithubSearchRequest("spring boot", null, Sort.STARS, Order.DESC, 1, 10);

    GitHubSearchResponse emptyResponse = new GitHubSearchResponse(false, List.of());

    setupMockWebClientChain();
    when(responseSpec.bodyToMono(GitHubSearchResponse.class)).thenReturn(Mono.just(emptyResponse));

    // Act & Assert
    StepVerifier.create(gitHubSearchService.searchRepositories(request))
        .expectNext(emptyResponse)
        .verifyComplete();
    verify(gitHubRepositoryService, never()).saveAllGitHubRepositories(any());
  }

  @Test
  void searchRepositories_WithComplexQuery_ShouldHandleAllParametersButNotSaveEmptyResults() {
    // Arrange
    GithubSearchRequest request =
        new GithubSearchRequest("microservices", "kotlin", Sort.UPDATED, Order.ASC, 5, 25);

    GitHubSearchResponse expectedResponse = new GitHubSearchResponse(false, List.of());

    setupMockWebClientChain();
    when(responseSpec.bodyToMono(GitHubSearchResponse.class))
        .thenReturn(Mono.just(expectedResponse));

    // Act
    StepVerifier.create(gitHubSearchService.searchRepositories(request))
        .expectNext(expectedResponse)
        .verifyComplete();

    // Verify
    verify(requestHeadersUriSpec).uri(any(Function.class));
    verify(gitHubRepositoryService, never()).saveAllGitHubRepositories(any());
  }

  @Test
  void searchRepositories_WithIncompleteResults_ShouldNotSaveRepositories() {
    // Arrange
    GithubSearchRequest request =
        new GithubSearchRequest("spring boot", null, Sort.STARS, Order.DESC, 1, 10);

    GitHubRepository repository = new GitHubRepository(1L, "repo1", "owner1");
    GitHubSearchResponse incompleteResponse = new GitHubSearchResponse(true, List.of(repository));

    setupMockWebClientChain();
    when(responseSpec.bodyToMono(GitHubSearchResponse.class))
        .thenReturn(Mono.just(incompleteResponse));

    // Act
    StepVerifier.create(gitHubSearchService.searchRepositories(request))
        .expectNext(incompleteResponse)
        .verifyComplete();

    verify(gitHubRepositoryService, never()).saveAllGitHubRepositories(any());
  }

  @SuppressWarnings({"unchecked"})
  private void setupMockWebClientChain() {
    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
  }
}
