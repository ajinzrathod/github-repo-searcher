package com.ajinz.githubsearch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.ajinz.githubsearch.dto.github.GitHubRepository;
import com.ajinz.githubsearch.repository.GitHubRepositoryRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GitHubRepositoryServiceTest {

  @Mock private GitHubRepositoryRepository gitHubRepositoryRepository;

  @InjectMocks private GitHubRepositoryService gitHubRepositoryService;

  private List<GitHubRepository> testRepositories;

  @BeforeEach
  void setUp() {
    // Create test repositories with different languages and star counts
    GitHubRepository javaRepo1 = createTestRepository(1L, "spring-boot", "Java", 45000, 15000);
    GitHubRepository javaRepo2 = createTestRepository(2L, "hibernate-orm", "Java", 25000, 8000);
    GitHubRepository pythonRepo1 = createTestRepository(3L, "django", "Python", 65000, 25000);
    GitHubRepository pythonRepo2 = createTestRepository(4L, "flask", "Python", 58000, 18000);
    GitHubRepository jsRepo = createTestRepository(5L, "react", "JavaScript", 200000, 40000);

    testRepositories = Arrays.asList(javaRepo1, javaRepo2, pythonRepo1, pythonRepo2, jsRepo);
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

  @Test
  void shouldReturnAllSavedRepositories() {
    // Given
    GitHubRepository repo1 = createGitHubRepository(1L, "spring-boot", "spring-projects");
    GitHubRepository repo2 = createGitHubRepository(2L, "react", "facebook");
    GitHubRepository repo3 = createGitHubRepository(3L, "vue", "vuejs");
    List<GitHubRepository> expectedRepositories = Arrays.asList(repo1, repo2, repo3);

    when(gitHubRepositoryRepository.findAll()).thenReturn(expectedRepositories);

    // When
    List<GitHubRepository> result = gitHubRepositoryService.getAllSavedRepositories();

    // Then
    assertNotNull(result);
    assertEquals(3, result.size());
    assertEquals(expectedRepositories, result);
    verify(gitHubRepositoryRepository).findAll();
  }

  @Test
  void shouldReturnEmptyListWhenNoRepositoriesSaved() {
    // Given
    List<GitHubRepository> emptyList = Collections.emptyList();
    when(gitHubRepositoryRepository.findAll()).thenReturn(emptyList);

    // When
    List<GitHubRepository> result = gitHubRepositoryService.getAllSavedRepositories();

    // Then
    assertNotNull(result);
    assertTrue(result.isEmpty());
    assertEquals(0, result.size());
    verify(gitHubRepositoryRepository).findAll();
  }

  @Test
  void shouldHandleRepositoryExceptionWhenGettingAllRepositories() {
    // Given
    when(gitHubRepositoryRepository.findAll())
        .thenThrow(new RuntimeException("Database connection error"));

    // When & Then
    assertThrows(RuntimeException.class, () -> gitHubRepositoryService.getAllSavedRepositories());

    verify(gitHubRepositoryRepository).findAll();
  }

  @Test
  void shouldReturnRepositoriesInCorrectOrder() {
    // Given - repositories returned by database in specific order
    GitHubRepository repo1 = createGitHubRepository(100L, "zzz-repo", "owner1");
    GitHubRepository repo2 = createGitHubRepository(50L, "aaa-repo", "owner2");
    GitHubRepository repo3 = createGitHubRepository(75L, "mmm-repo", "owner3");
    List<GitHubRepository> databaseOrder = Arrays.asList(repo1, repo2, repo3);

    when(gitHubRepositoryRepository.findAll()).thenReturn(databaseOrder);

    // When
    List<GitHubRepository> result = gitHubRepositoryService.getAllSavedRepositories();

    // Then - should return repositories in the same order as database
    assertNotNull(result);
    assertEquals(3, result.size());
    assertEquals(repo1, result.get(0));
    assertEquals(repo2, result.get(1));
    assertEquals(repo3, result.get(2));
    verify(gitHubRepositoryRepository).findAll();
  }

  @Test
  void getFilteredRepositories_WithNoFilters_ShouldReturnAllRepositories() {
    // Arrange
    when(gitHubRepositoryRepository.findRepositoriesWithFilters(null, null, "stars"))
        .thenReturn(testRepositories);

    // Act
    List<GitHubRepository> result =
        gitHubRepositoryService.getFilteredRepositories(null, null, "stars");

    // Assert
    assertEquals(5, result.size());
    verify(gitHubRepositoryRepository).findRepositoriesWithFilters(null, null, "stars");
  }

  @Test
  void getFilteredRepositories_WithLanguageFilter_ShouldFilterByLanguage() {
    // Arrange
    List<GitHubRepository> javaRepositories =
        Arrays.asList(testRepositories.get(0), testRepositories.get(1));
    when(gitHubRepositoryRepository.findRepositoriesWithFilters("Java", null, "stars"))
        .thenReturn(javaRepositories);

    // Act
    List<GitHubRepository> result =
        gitHubRepositoryService.getFilteredRepositories("Java", null, "stars");

    // Assert
    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(repo -> "Java".equals(repo.getProgrammingLanguage())));
    verify(gitHubRepositoryRepository).findRepositoriesWithFilters("Java", null, "stars");
  }

  @Test
  void getFilteredRepositories_WithMinStarsFilter_ShouldFilterByMinStars() {
    // Arrange
    List<GitHubRepository> highStarRepositories =
        Arrays.asList(testRepositories.get(2), testRepositories.get(3), testRepositories.get(4));
    when(gitHubRepositoryRepository.findRepositoriesWithFilters(null, 50000, "stars"))
        .thenReturn(highStarRepositories);

    // Act
    List<GitHubRepository> result =
        gitHubRepositoryService.getFilteredRepositories(null, 50000, "stars");

    // Assert
    assertEquals(3, result.size());
    assertTrue(result.stream().allMatch(repo -> repo.getStarsCount() >= 50000));
    verify(gitHubRepositoryRepository).findRepositoriesWithFilters(null, 50000, "stars");
  }

  @Test
  void getFilteredRepositories_WithLanguageAndMinStarsFilter_ShouldApplyBothFilters() {
    // Arrange
    List<GitHubRepository> filteredRepositories =
        Arrays.asList(testRepositories.get(2), testRepositories.get(3));
    when(gitHubRepositoryRepository.findRepositoriesWithFilters("Python", 50000, "stars"))
        .thenReturn(filteredRepositories);

    // Act
    List<GitHubRepository> result =
        gitHubRepositoryService.getFilteredRepositories("Python", 50000, "stars");

    // Assert
    assertEquals(2, result.size());
    assertTrue(
        result.stream()
            .allMatch(
                repo ->
                    "Python".equals(repo.getProgrammingLanguage())
                        && repo.getStarsCount() >= 50000));
    verify(gitHubRepositoryRepository).findRepositoriesWithFilters("Python", 50000, "stars");
  }

  @Test
  void getFilteredRepositories_WithCustomSortBy_ShouldPassSortParameter() {
    // Arrange
    when(gitHubRepositoryRepository.findRepositoriesWithFilters(null, null, "forks"))
        .thenReturn(testRepositories);

    // Act
    List<GitHubRepository> result =
        gitHubRepositoryService.getFilteredRepositories(null, null, "forks");

    // Assert
    assertEquals(5, result.size());
    verify(gitHubRepositoryRepository).findRepositoriesWithFilters(null, null, "forks");
  }

  @Test
  void getFilteredRepositories_WithNullSortBy_ShouldDefaultToStars() {
    // Arrange
    when(gitHubRepositoryRepository.findRepositoriesWithFilters(null, null, "stars"))
        .thenReturn(testRepositories);

    // Act
    List<GitHubRepository> result =
        gitHubRepositoryService.getFilteredRepositories(null, null, null);

    // Assert
    assertEquals(5, result.size());
    verify(gitHubRepositoryRepository).findRepositoriesWithFilters(null, null, "stars");
  }

  @Test
  void getFilteredRepositories_WithEmptySortBy_ShouldDefaultToStars() {
    // Arrange
    when(gitHubRepositoryRepository.findRepositoriesWithFilters(null, null, "stars"))
        .thenReturn(testRepositories);

    // Act
    List<GitHubRepository> result = gitHubRepositoryService.getFilteredRepositories(null, null, "");

    // Assert
    assertEquals(5, result.size());
    verify(gitHubRepositoryRepository).findRepositoriesWithFilters(null, null, "stars");
  }

  @Test
  void getFilteredRepositories_WithAllParametersSet_ShouldPassAllParameters() {
    // Arrange
    List<GitHubRepository> filteredResult = Collections.singletonList(testRepositories.getFirst());
    when(gitHubRepositoryRepository.findRepositoriesWithFilters("Java", 30000, "name"))
        .thenReturn(filteredResult);

    // Act
    List<GitHubRepository> result =
        gitHubRepositoryService.getFilteredRepositories("Java", 30000, "name");

    // Assert
    assertEquals(1, result.size());
    verify(gitHubRepositoryRepository).findRepositoriesWithFilters("Java", 30000, "name");
  }

  private GitHubRepository createGitHubRepository(
      Long githubRepoId, String repoName, String ownerName) {
    return new GitHubRepository(githubRepoId, repoName, ownerName);
  }

  private GitHubRepository createTestRepository(
      Long id, String name, String language, int stars, int forks) {
    return new GitHubRepository(
        id, name, "testowner", "Test repository", language, stars, forks, LocalDateTime.now());
  }
}
