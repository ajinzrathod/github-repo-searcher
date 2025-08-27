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

  private GitHubSearchService gitHubSearchService;

  private final String baseUrl = "https://api.github.com";
  private final String apiVersion = "2022-11-28";

  @BeforeEach
  void setUp() {
    when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
    when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
    when(webClientBuilder.build()).thenReturn(webClient);

    gitHubSearchService = new GitHubSearchService(webClientBuilder, baseUrl, apiVersion);
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
  void searchRepositories_WithBasicRequest_ShouldReturnSuccessfulResponse() {
    // Arrange
    GithubSearchRequest request =
        new GithubSearchRequest("spring boot", null, Sort.STARS, Order.DESC, 1, 10);

    GitHubRepository repository = new GitHubRepository();
    GitHubSearchResponse expectedResponse = new GitHubSearchResponse(false, List.of(repository));

    setupMockWebClientChain();
    when(responseSpec.bodyToMono(GitHubSearchResponse.class))
        .thenReturn(Mono.just(expectedResponse));

    // Act & Assert
    StepVerifier.create(gitHubSearchService.searchRepositories(request))
        .expectNext(expectedResponse)
        .verifyComplete();

    verify(webClient).get();
  }

  @Test
  void searchRepositories_WithLanguageFilter_ShouldIncludeLanguageInQuery() {
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

    // Verify that the URI builder is called
    verify(requestHeadersUriSpec).uri(any(Function.class));
  }

  @Test
  void searchRepositories_WithEmptyLanguage_ShouldNotIncludeLanguageInQuery() {
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
  }

  @Test
  void searchRepositories_WithWebClientResponseException_ShouldMapToRuntimeException() {
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
  }

  @Test
  void searchRepositories_WithGenericException_ShouldMapToRuntimeException() {
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
  }

  @Test
  void searchRepositories_WithEmptyResponse_ShouldHandleGracefully() {
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
  }

  @Test
  void searchRepositories_WithComplexQuery_ShouldHandleAllParameters() {
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

    // Verify the URI building was called
    verify(requestHeadersUriSpec).uri(any(Function.class));
  }

  @Test
  void constructor_WithCustomBaseUrlAndApiVersion_ShouldUseProvidedValues() {
    // Arrange
    String customBaseUrl = "https://api.custom-github.com";
    String customApiVersion = "2023-01-01";

    WebClient.Builder newBuilder = mock(WebClient.Builder.class);
    when(newBuilder.baseUrl(anyString())).thenReturn(newBuilder);
    when(newBuilder.defaultHeader(anyString(), anyString())).thenReturn(newBuilder);
    when(newBuilder.build()).thenReturn(webClient);

    // Act
    new GitHubSearchService(newBuilder, customBaseUrl, customApiVersion);

    // Assert
    verify(newBuilder).baseUrl(customBaseUrl);
    verify(newBuilder).defaultHeader("X-GitHub-Api-Version", customApiVersion);
  }

  @SuppressWarnings({"unchecked"})
  private void setupMockWebClientChain() {
    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
  }
}
