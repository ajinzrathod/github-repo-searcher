package com.ajinz.githubsearch.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ajinz.githubsearch.dto.github.GitHubRepository;
import com.ajinz.githubsearch.dto.github.GithubSearchRequest;
import com.ajinz.githubsearch.dto.github.Order;
import com.ajinz.githubsearch.dto.github.Sort;
import com.ajinz.githubsearch.repository.GitHubRepositoryRepository;
import com.ajinz.githubsearch.service.GitHubRepositoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integrationTest")
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.flyway.enabled=true",
      "spring.jpa.hibernate.ddl-auto=validate"
    })
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:cleanup.sql")
class SearchControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private GitHubRepositoryRepository gitHubRepositoryRepository;

  @Autowired private GitHubRepositoryService gitHubRepositoryService;

  @Test
  void shouldReturnHealthStatus() throws Exception {
    mockMvc
        .perform(get("/api/github/health"))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"));
  }

  @Test
  void shouldSearchRepositoriesWithPostRequest() throws Exception {
    GithubSearchRequest searchRequest =
        new GithubSearchRequest("spring", "java", Sort.STARS, Order.DESC, 1, 5);

    MvcResult mvcResult =
        mockMvc
            .perform(
                post("/api/github/search/repositories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(searchRequest)))
            .andExpect(request().asyncStarted())
            .andReturn();

    mockMvc
        .perform(asyncDispatch(mvcResult))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.items").isArray());
  }

  @Test
  void shouldReturnValidationErrorForInvalidRequest() throws Exception {
    GithubSearchRequest invalidRequest =
        new GithubSearchRequest("OpenCV", null, Sort.FORKS, Order.DESC, 35, 200);

    mockMvc
        .perform(
            post("/api/github/search/repositories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
  }

  @Test
  @Transactional
  @Rollback
  void shouldPersistRepositoriesToDatabaseAndAvoidDuplicatesOnSubsequentSearches() {
    // Arrange: Create test repositories that would be returned by GitHub API
    GitHubRepository repo1 = new GitHubRepository(12345L, "spring-boot", "spring-projects");
    GitHubRepository repo2 = new GitHubRepository(67890L, "spring-framework", "spring-projects");
    GitHubRepository repo3 = new GitHubRepository(11111L, "spring-data", "spring-projects");

    List<GitHubRepository> firstSearchResults = Arrays.asList(repo1, repo2);
    List<GitHubRepository> secondSearchResults =
        Arrays.asList(repo1, repo2, repo3); // repo1 and repo2 are duplicates

    // Verify database is empty initially
    assertEquals(0, gitHubRepositoryRepository.count(), "Database should be empty initially");

    // Act 1: Simulate first search - save initial repositories
    gitHubRepositoryService.saveAllGitHubRepositories(firstSearchResults);

    // Assert 1: Verify repositories are persisted to database
    assertEquals(
        2, gitHubRepositoryRepository.count(), "Should have 2 repositories after first search");
    assertTrue(
        gitHubRepositoryRepository.findByGithubRepoId(12345L).isPresent(),
        "Repository with ID 12345 should exist");
    assertTrue(
        gitHubRepositoryRepository.findByGithubRepoId(67890L).isPresent(),
        "Repository with ID 67890 should exist");
    assertFalse(
        gitHubRepositoryRepository.findByGithubRepoId(11111L).isPresent(),
        "Repository with ID 11111 should not exist yet");

    // Act 2: Simulate second search with overlapping results
    gitHubRepositoryService.saveAllGitHubRepositories(secondSearchResults);

    // Assert 2: Verify only new repository is added (no duplicates)
    assertEquals(
        3,
        gitHubRepositoryRepository.count(),
        "Should have 3 repositories total after second search (only 1 new added)");
    assertTrue(
        gitHubRepositoryRepository.findByGithubRepoId(12345L).isPresent(),
        "Repository with ID 12345 should still exist");
    assertTrue(
        gitHubRepositoryRepository.findByGithubRepoId(67890L).isPresent(),
        "Repository with ID 67890 should still exist");
    assertTrue(
        gitHubRepositoryRepository.findByGithubRepoId(11111L).isPresent(),
        "Repository with ID 11111 should now exist");

    // Verify the actual repository data integrity
    GitHubRepository savedRepo1 = gitHubRepositoryRepository.findByGithubRepoId(12345L).get();
    assertEquals("spring-boot", savedRepo1.getRepoName());
    assertEquals("spring-projects", savedRepo1.getOwnerName());

    GitHubRepository savedRepo3 = gitHubRepositoryRepository.findByGithubRepoId(11111L).get();
    assertEquals("spring-data", savedRepo3.getRepoName());
    assertEquals("spring-projects", savedRepo3.getOwnerName());
  }

  @Test
  @Transactional
  @Rollback
  void shouldHandleEmptySearchResultsWithoutAffectingDatabase() {
    // Arrange: Add some existing repositories
    GitHubRepository existingRepo = new GitHubRepository(99999L, "existing-repo", "existing-owner");
    gitHubRepositoryService.saveAllGitHubRepositories(List.of(existingRepo));

    assertEquals(1, gitHubRepositoryRepository.count(), "Should have 1 repository initially");

    // Act: Save empty results
    gitHubRepositoryService.saveAllGitHubRepositories(List.of());

    // Assert: Database count should remain unchanged
    assertEquals(
        1,
        gitHubRepositoryRepository.count(),
        "Database count should remain unchanged after empty save");
    assertTrue(
        gitHubRepositoryRepository.findByGithubRepoId(99999L).isPresent(),
        "Existing repository should still be present");
  }

  @Test
  @Transactional
  @Rollback
  void shouldHandleLargeDatasetDeduplicationEfficiently() {
    // Arrange: Create a mix of new and existing repositories
    List<GitHubRepository> existingRepos =
        Arrays.asList(
            new GitHubRepository(1L, "repo1", "owner1"),
            new GitHubRepository(3L, "repo3", "owner3"),
            new GitHubRepository(5L, "repo5", "owner5"));

    List<GitHubRepository> newBatch =
        Arrays.asList(
            new GitHubRepository(1L, "repo1", "owner1"), // duplicate
            new GitHubRepository(2L, "repo2", "owner2"), // new
            new GitHubRepository(3L, "repo3", "owner3"), // duplicate
            new GitHubRepository(4L, "repo4", "owner4"), // new
            new GitHubRepository(5L, "repo5", "owner5"), // duplicate
            new GitHubRepository(6L, "repo6", "owner6") // new
            );

    // Act: Save existing repositories first
    gitHubRepositoryService.saveAllGitHubRepositories(existingRepos);
    assertEquals(3, gitHubRepositoryRepository.count(), "Should have 3 repositories initially");

    // Save new batch with duplicates
    gitHubRepositoryService.saveAllGitHubRepositories(newBatch);

    // Assert: Only new repositories should be added
    assertEquals(
        6, gitHubRepositoryRepository.count(), "Should have 6 total repositories (3 new added)");

    // Verify all repositories exist
    for (long i = 1; i <= 6; i++) {
      assertTrue(
          gitHubRepositoryRepository.findByGithubRepoId(i).isPresent(),
          "Repository with ID " + i + " should exist");
    }
  }

  @Test
  @Transactional
  @Rollback
  void shouldReturnAllSavedRepositories() throws Exception {
    // Arrange: Save some test repositories to the database
    GitHubRepository repo1 = new GitHubRepository(12345L, "spring-boot", "spring-projects");
    GitHubRepository repo2 = new GitHubRepository(67890L, "spring-framework", "spring-projects");
    GitHubRepository repo3 = new GitHubRepository(11111L, "react", "facebook");

    List<GitHubRepository> testRepositories = Arrays.asList(repo1, repo2, repo3);
    gitHubRepositoryService.saveAllGitHubRepositories(testRepositories);

    // Act & Assert: Call the API endpoint and verify the response
    mockMvc
        .perform(get("/api/github/repositories"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].name").exists())
        .andExpect(jsonPath("$[0].owner").exists());
  }

  @Test
  void shouldReturnEmptyListWhenNoRepositoriesSaved() throws Exception {
    // Act & Assert: Call the API endpoint when database is empty
    mockMvc
        .perform(get("/api/github/repositories"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @Transactional
  @Rollback
  void shouldFilterRepositoriesByLanguage() throws Exception {
    // Arrange: Create test repositories with different languages
    GitHubRepository javaRepo1 =
        createTestRepositoryWithDetails(1L, "spring-boot", "Java", 45000, 15000);
    GitHubRepository javaRepo2 =
        createTestRepositoryWithDetails(2L, "hibernate-orm", "Java", 25000, 8000);
    GitHubRepository pythonRepo =
        createTestRepositoryWithDetails(3L, "django", "Python", 65000, 25000);
    GitHubRepository jsRepo =
        createTestRepositoryWithDetails(4L, "react", "JavaScript", 200000, 40000);

    List<GitHubRepository> allRepositories =
        Arrays.asList(javaRepo1, javaRepo2, pythonRepo, jsRepo);
    gitHubRepositoryService.saveAllGitHubRepositories(allRepositories);

    // Act & Assert: Filter by Java language
    mockMvc
        .perform(get("/api/github/repositories").param("language", "Java"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].language").value("Java"))
        .andExpect(jsonPath("$[1].language").value("Java"));
  }

  @Test
  @Transactional
  @Rollback
  void shouldFilterRepositoriesByMinStars() throws Exception {
    // Arrange: Create test repositories with different star counts
    GitHubRepository lowStarRepo =
        createTestRepositoryWithDetails(1L, "small-project", "Java", 500, 100);
    GitHubRepository mediumStarRepo =
        createTestRepositoryWithDetails(2L, "medium-project", "Python", 25000, 8000);
    GitHubRepository highStarRepo1 =
        createTestRepositoryWithDetails(3L, "popular-project", "JavaScript", 65000, 25000);
    GitHubRepository highStarRepo2 =
        createTestRepositoryWithDetails(4L, "very-popular", "Python", 200000, 40000);

    List<GitHubRepository> allRepositories =
        Arrays.asList(lowStarRepo, mediumStarRepo, highStarRepo1, highStarRepo2);
    gitHubRepositoryService.saveAllGitHubRepositories(allRepositories);

    // Act & Assert: Filter by minimum 50000 stars
    mockMvc
        .perform(get("/api/github/repositories").param("minStars", "50000"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].stargazers_count").value(greaterThanOrEqualTo(50000)))
        .andExpect(jsonPath("$[1].stargazers_count").value(greaterThanOrEqualTo(50000)));
  }

  @Test
  @Transactional
  @Rollback
  void shouldFilterRepositoriesByLanguageAndMinStars() throws Exception {
    // Arrange: Create test repositories
    GitHubRepository javaLowStars =
        createTestRepositoryWithDetails(1L, "java-small", "Java", 1000, 200);
    GitHubRepository javaHighStars =
        createTestRepositoryWithDetails(2L, "java-popular", "Java", 75000, 15000);
    GitHubRepository pythonLowStars =
        createTestRepositoryWithDetails(3L, "python-small", "Python", 2000, 500);
    GitHubRepository pythonHighStars =
        createTestRepositoryWithDetails(4L, "python-popular", "Python", 85000, 25000);
    GitHubRepository jsHighStars =
        createTestRepositoryWithDetails(5L, "js-popular", "JavaScript", 95000, 30000);

    List<GitHubRepository> allRepositories =
        Arrays.asList(javaLowStars, javaHighStars, pythonLowStars, pythonHighStars, jsHighStars);
    gitHubRepositoryService.saveAllGitHubRepositories(allRepositories);

    // Act & Assert: Filter by Python language and minimum 50000 stars
    mockMvc
        .perform(
            get("/api/github/repositories").param("language", "Python").param("minStars", "50000"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].language").value("Python"))
        .andExpect(jsonPath("$[0].stargazers_count").value(85000))
        .andExpect(jsonPath("$[0].name").value("python-popular"));
  }

  @Test
  @Transactional
  @Rollback
  void shouldSortRepositoriesByStarsDescending() throws Exception {
    // Arrange: Create repositories with different star counts
    GitHubRepository repo1 =
        createTestRepositoryWithDetails(1L, "repo-medium", "Java", 25000, 8000);
    GitHubRepository repo2 =
        createTestRepositoryWithDetails(2L, "repo-highest", "Python", 85000, 25000);
    GitHubRepository repo3 =
        createTestRepositoryWithDetails(3L, "repo-lowest", "JavaScript", 5000, 2000);

    List<GitHubRepository> allRepositories = Arrays.asList(repo1, repo2, repo3);
    gitHubRepositoryService.saveAllGitHubRepositories(allRepositories);

    // Act & Assert: Default sort should be by stars descending
    mockMvc
        .perform(get("/api/github/repositories"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].stargazers_count").value(85000))
        .andExpect(jsonPath("$[1].stargazers_count").value(25000))
        .andExpect(jsonPath("$[2].stargazers_count").value(5000));
  }

  @Test
  @Transactional
  @Rollback
  void shouldSortRepositoriesByForks() throws Exception {
    // Arrange: Create repositories with different fork counts
    GitHubRepository repo1 =
        createTestRepositoryWithDetails(1L, "repo-low-forks", "Java", 25000, 5000);
    GitHubRepository repo2 =
        createTestRepositoryWithDetails(2L, "repo-high-forks", "Python", 30000, 40000);
    GitHubRepository repo3 =
        createTestRepositoryWithDetails(3L, "repo-medium-forks", "JavaScript", 35000, 15000);

    List<GitHubRepository> allRepositories = Arrays.asList(repo1, repo2, repo3);
    gitHubRepositoryService.saveAllGitHubRepositories(allRepositories);

    // Act & Assert: Sort by forks descending
    mockMvc
        .perform(get("/api/github/repositories").param("sort", "forks"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].forks_count").value(40000))
        .andExpect(jsonPath("$[1].forks_count").value(15000))
        .andExpect(jsonPath("$[2].forks_count").value(5000));
  }

  @Test
  @Transactional
  @Rollback
  void shouldSortRepositoriesByName() throws Exception {
    // Arrange: Create repositories with different names
    GitHubRepository repoZ =
        createTestRepositoryWithDetails(1L, "zebra-project", "Java", 25000, 8000);
    GitHubRepository repoA =
        createTestRepositoryWithDetails(2L, "alpha-project", "Python", 30000, 10000);
    GitHubRepository repoM =
        createTestRepositoryWithDetails(3L, "middle-project", "JavaScript", 35000, 12000);

    List<GitHubRepository> allRepositories = Arrays.asList(repoZ, repoA, repoM);
    gitHubRepositoryService.saveAllGitHubRepositories(allRepositories);

    // Act & Assert: Sort by name ascending
    mockMvc
        .perform(get("/api/github/repositories").param("sort", "name"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].name").value("alpha-project"))
        .andExpect(jsonPath("$[1].name").value("middle-project"))
        .andExpect(jsonPath("$[2].name").value("zebra-project"));
  }

  @Test
  @Transactional
  @Rollback
  void shouldReturnEmptyResultWhenNoRepositoriesMatchFilter() throws Exception {
    // Arrange: Create repositories that won't match the filter
    GitHubRepository javaRepo =
        createTestRepositoryWithDetails(1L, "java-project", "Java", 1000, 200);
    GitHubRepository pythonRepo =
        createTestRepositoryWithDetails(2L, "python-project", "Python", 2000, 500);

    List<GitHubRepository> allRepositories = Arrays.asList(javaRepo, pythonRepo);
    gitHubRepositoryService.saveAllGitHubRepositories(allRepositories);

    // Act & Assert: Filter by non-existent language
    mockMvc
        .perform(get("/api/github/repositories").param("language", "Rust"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @Transactional
  @Rollback
  void shouldReturnAllRepositoriesWhenNoFiltersApplied() throws Exception {
    // Arrange: Create diverse repositories
    GitHubRepository repo1 =
        createTestRepositoryWithDetails(1L, "project-one", "Java", 15000, 3000);
    GitHubRepository repo2 =
        createTestRepositoryWithDetails(2L, "project-two", "Python", 25000, 8000);
    GitHubRepository repo3 =
        createTestRepositoryWithDetails(3L, "project-three", "JavaScript", 35000, 12000);

    List<GitHubRepository> allRepositories = Arrays.asList(repo1, repo2, repo3);
    gitHubRepositoryService.saveAllGitHubRepositories(allRepositories);

    // Act & Assert: No filters should return all repositories
    mockMvc
        .perform(get("/api/github/repositories"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  @Transactional
  @Rollback
  void shouldHandleComplexFilteringScenario() throws Exception {
    // Arrange: Create a comprehensive dataset
    GitHubRepository[] repositories = {
      createTestRepositoryWithDetails(1L, "spring-boot", "Java", 65000, 15000),
      createTestRepositoryWithDetails(2L, "hibernate-orm", "Java", 25000, 8000),
      createTestRepositoryWithDetails(3L, "small-java-lib", "Java", 500, 100),
      createTestRepositoryWithDetails(4L, "django", "Python", 70000, 25000),
      createTestRepositoryWithDetails(5L, "flask", "Python", 58000, 18000),
      createTestRepositoryWithDetails(6L, "python-util", "Python", 1200, 300),
      createTestRepositoryWithDetails(7L, "react", "JavaScript", 200000, 40000),
      createTestRepositoryWithDetails(8L, "vue", "JavaScript", 195000, 35000),
      createTestRepositoryWithDetails(9L, "small-js-tool", "JavaScript", 800, 150)
    };

    gitHubRepositoryService.saveAllGitHubRepositories(Arrays.asList(repositories));

    // Test 1: Filter Java repositories with at least 20000 stars, sorted by stars
    mockMvc
        .perform(
            get("/api/github/repositories")
                .param("language", "Java")
                .param("minStars", "20000")
                .param("sort", "stars"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].stargazers_count").value(65000))
        .andExpect(jsonPath("$[1].stargazers_count").value(25000));

    // Test 2: Filter Python repositories with at least 50000 stars, sorted by name
    mockMvc
        .perform(
            get("/api/github/repositories")
                .param("language", "Python")
                .param("minStars", "50000")
                .param("sort", "name"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("django"))
        .andExpect(jsonPath("$[1].name").value("flask"));

    // Test 3: Filter all repositories with at least 100000 stars
    mockMvc
        .perform(get("/api/github/repositories").param("minStars", "100000"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].language").value("JavaScript"))
        .andExpect(jsonPath("$[1].language").value("JavaScript"));
  }

  @Test
  void shouldHandleInvalidMinStarsParameter() throws Exception {
    // Act & Assert: Invalid minStars parameter should return internal server error
    mockMvc
        .perform(get("/api/github/repositories").param("minStars", "not-a-number"))
        .andExpect(status().isInternalServerError());
  }

  @Test
  @Transactional
  @Rollback
  void shouldHandleEdgeCaseFilters() throws Exception {
    // Arrange: Create repositories for edge case testing
    GitHubRepository repo1 = createTestRepositoryWithDetails(1L, "zero-stars", "Java", 0, 0);
    GitHubRepository repo2 =
        createTestRepositoryWithDetails(2L, "negative-test", "Python", 1000, 200);

    List<GitHubRepository> repositories = Arrays.asList(repo1, repo2);
    gitHubRepositoryService.saveAllGitHubRepositories(repositories);

    // Test 1: Filter with 0 minStars (should include zero-star repo)
    mockMvc
        .perform(get("/api/github/repositories").param("minStars", "0"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2));

    // Test 2: Filter with negative minStars (should include all repos)
    mockMvc
        .perform(get("/api/github/repositories").param("minStars", "-100"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2));

    // Test 3: Filter with very high minStars (should return empty)
    mockMvc
        .perform(get("/api/github/repositories").param("minStars", "999999"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(0));
  }

  private GitHubRepository createTestRepositoryWithDetails(
      Long id, String name, String language, int stars, int forks) {
    return new GitHubRepository(
        id,
        name,
        "testowner",
        "Test repository: " + name,
        language,
        stars,
        forks,
        LocalDateTime.now().minusDays(id) // Different update dates for sorting tests
        );
  }
}
