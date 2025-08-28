package com.ajinz.githubsearch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.ajinz.githubsearch.dto.github.GitHubRepository;
import com.ajinz.githubsearch.repository.GitHubRepositoryRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class GitHubRepositoryServiceTest {

  @Mock private GitHubRepositoryRepository gitHubRepositoryRepository;

  private GitHubRepositoryService gitHubRepositoryService;

  @BeforeEach
  void setUp() {
    try (var mocks = MockitoAnnotations.openMocks(this)) {
      gitHubRepositoryService = new GitHubRepositoryService(gitHubRepositoryRepository);
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize mocks", e);
    }
  }

  @Test
  void shouldSaveAllRepositoriesWhenNoneExist() {
    // Given
    GitHubRepository repo1 = createGitHubRepository(1L, "repo1", "owner1");
    GitHubRepository repo2 = createGitHubRepository(2L, "repo2", "owner2");
    List<GitHubRepository> repositories = Arrays.asList(repo1, repo2);

    when(gitHubRepositoryRepository.findByGithubRepoId(1L)).thenReturn(Optional.empty());
    when(gitHubRepositoryRepository.findByGithubRepoId(2L)).thenReturn(Optional.empty());

    // When
    gitHubRepositoryService.saveAllGitHubRepositories(repositories);

    // Then
    verify(gitHubRepositoryRepository).findByGithubRepoId(1L);
    verify(gitHubRepositoryRepository).findByGithubRepoId(2L);
    verify(gitHubRepositoryRepository).saveAll(repositories);
  }

  @Test
  void shouldNotSaveRepositoriesWhenAllExist() {
    // Given
    GitHubRepository repo1 = createGitHubRepository(1L, "repo1", "owner1");
    GitHubRepository repo2 = createGitHubRepository(2L, "repo2", "owner2");
    List<GitHubRepository> repositories = Arrays.asList(repo1, repo2);

    when(gitHubRepositoryRepository.findByGithubRepoId(1L)).thenReturn(Optional.of(repo1));
    when(gitHubRepositoryRepository.findByGithubRepoId(2L)).thenReturn(Optional.of(repo2));

    // When
    gitHubRepositoryService.saveAllGitHubRepositories(repositories);

    // Then
    verify(gitHubRepositoryRepository).findByGithubRepoId(1L);
    verify(gitHubRepositoryRepository).findByGithubRepoId(2L);
    verify(gitHubRepositoryRepository).saveAll(Collections.emptyList());
  }

  @Test
  void shouldSaveOnlyNewRepositoriesWhenSomeExist() {
    // Given
    GitHubRepository existingRepo = createGitHubRepository(1L, "existing-repo", "owner1");
    GitHubRepository newRepo = createGitHubRepository(2L, "new-repo", "owner2");
    List<GitHubRepository> repositories = Arrays.asList(existingRepo, newRepo);

    when(gitHubRepositoryRepository.findByGithubRepoId(1L)).thenReturn(Optional.of(existingRepo));
    when(gitHubRepositoryRepository.findByGithubRepoId(2L)).thenReturn(Optional.empty());

    // When
    gitHubRepositoryService.saveAllGitHubRepositories(repositories);

    // Then
    verify(gitHubRepositoryRepository).findByGithubRepoId(1L);
    verify(gitHubRepositoryRepository).findByGithubRepoId(2L);
    verify(gitHubRepositoryRepository).saveAll(Collections.singletonList(newRepo));
  }

  @Test
  void shouldHandleEmptyRepositoryList() {
    // Given
    List<GitHubRepository> emptyList = Collections.emptyList();

    // When
    gitHubRepositoryService.saveAllGitHubRepositories(emptyList);

    // Then
    verify(gitHubRepositoryRepository, never()).findByGithubRepoId(anyLong());
    verify(gitHubRepositoryRepository).saveAll(emptyList);
  }

  @Test
  void shouldHandleNullRepositoryId() {
    // Given
    GitHubRepository repoWithNullId = createGitHubRepository(null, "repo", "owner");
    List<GitHubRepository> repositories = Collections.singletonList(repoWithNullId);

    when(gitHubRepositoryRepository.findByGithubRepoId(null)).thenReturn(Optional.empty());

    // When
    gitHubRepositoryService.saveAllGitHubRepositories(repositories);

    // Then
    verify(gitHubRepositoryRepository).findByGithubRepoId(null);
    verify(gitHubRepositoryRepository).saveAll(repositories);
  }

  @Test
  void shouldHandleRepositoryRepositoryException() {
    // Given
    GitHubRepository repo = createGitHubRepository(1L, "repo", "owner");
    List<GitHubRepository> repositories = Collections.singletonList(repo);

    when(gitHubRepositoryRepository.findByGithubRepoId(1L))
        .thenThrow(new RuntimeException("Database error"));

    // When & Then
    assertThrows(
        RuntimeException.class,
        () -> gitHubRepositoryService.saveAllGitHubRepositories(repositories));

    verify(gitHubRepositoryRepository).findByGithubRepoId(1L);
    verify(gitHubRepositoryRepository, never()).saveAll(any());
  }

  @Test
  void shouldHandleSaveAllException() {
    // Given
    GitHubRepository repo = createGitHubRepository(1L, "repo", "owner");
    List<GitHubRepository> repositories = Collections.singletonList(repo);

    when(gitHubRepositoryRepository.findByGithubRepoId(1L)).thenReturn(Optional.empty());
    when(gitHubRepositoryRepository.saveAll(any())).thenThrow(new RuntimeException("Save error"));

    // When & Then
    assertThrows(
        RuntimeException.class,
        () -> gitHubRepositoryService.saveAllGitHubRepositories(repositories));

    verify(gitHubRepositoryRepository).findByGithubRepoId(1L);
    verify(gitHubRepositoryRepository).saveAll(repositories);
  }

  @Test
  void shouldFilterCorrectlyWithLargeDataset() {
    // Given
    GitHubRepository existingRepo1 = createGitHubRepository(1L, "repo1", "owner1");
    GitHubRepository existingRepo2 = createGitHubRepository(3L, "repo3", "owner3");
    GitHubRepository newRepo1 = createGitHubRepository(2L, "repo2", "owner2");
    GitHubRepository newRepo2 = createGitHubRepository(4L, "repo4", "owner4");

    List<GitHubRepository> repositories =
        Arrays.asList(existingRepo1, newRepo1, existingRepo2, newRepo2);
    List<GitHubRepository> expectedNewRepos = Arrays.asList(newRepo1, newRepo2);

    when(gitHubRepositoryRepository.findByGithubRepoId(1L)).thenReturn(Optional.of(existingRepo1));
    when(gitHubRepositoryRepository.findByGithubRepoId(2L)).thenReturn(Optional.empty());
    when(gitHubRepositoryRepository.findByGithubRepoId(3L)).thenReturn(Optional.of(existingRepo2));
    when(gitHubRepositoryRepository.findByGithubRepoId(4L)).thenReturn(Optional.empty());

    // When
    gitHubRepositoryService.saveAllGitHubRepositories(repositories);

    // Then
    verify(gitHubRepositoryRepository).saveAll(expectedNewRepos);
  }

  private GitHubRepository createGitHubRepository(
      Long githubRepoId, String repoName, String ownerName) {
    return new GitHubRepository(githubRepoId, repoName, ownerName);
  }
}
