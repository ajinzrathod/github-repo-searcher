package com.ajinz.githubsearch.controller;

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
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
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
}
