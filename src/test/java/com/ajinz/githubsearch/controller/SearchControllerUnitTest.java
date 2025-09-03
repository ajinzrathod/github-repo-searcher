package com.ajinz.githubsearch.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ajinz.githubsearch.dto.github.GitHubRepository;
import com.ajinz.githubsearch.service.GitHubRepositoryService;
import com.ajinz.githubsearch.service.GitHubSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SearchController.class)
@ActiveProfiles("test")
class SearchControllerUnitTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private GitHubRepositoryService gitHubRepositoryService;

  @MockitoBean private GitHubSearchService gitHubSearchService;

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
  void getFilteredRepositories_WithNoFilters_ShouldReturnAllRepositories() throws Exception {
    // Arrange
    when(gitHubRepositoryService.getFilteredRepositories(null, null, "stars"))
        .thenReturn(testRepositories);

    // Act & Assert
    mockMvc
        .perform(get("/api/github/repositories"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(5))
        .andExpect(jsonPath("$[0].name").value("spring-boot"))
        .andExpect(jsonPath("$[0].language").value("Java"))
        .andExpect(jsonPath("$[0].stargazers_count").value(45000));

    verify(gitHubRepositoryService).getFilteredRepositories(null, null, "stars");
  }

  @Test
  void getFilteredRepositories_WithLanguageFilter_ShouldFilterByLanguage() throws Exception {
    // Arrange
    List<GitHubRepository> javaRepositories =
        Arrays.asList(testRepositories.get(0), testRepositories.get(1));
    when(gitHubRepositoryService.getFilteredRepositories("Java", null, "stars"))
        .thenReturn(javaRepositories);

    // Act & Assert
    mockMvc
        .perform(get("/api/github/repositories").param("language", "Java"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].language").value("Java"))
        .andExpect(jsonPath("$[1].language").value("Java"));

    verify(gitHubRepositoryService).getFilteredRepositories("Java", null, "stars");
  }

  @Test
  void getFilteredRepositories_WithMinStarsFilter_ShouldFilterByMinStars() throws Exception {
    // Arrange
    List<GitHubRepository> highStarRepositories =
        Arrays.asList(testRepositories.get(2), testRepositories.get(3), testRepositories.get(4));
    when(gitHubRepositoryService.getFilteredRepositories(null, 50000, "stars"))
        .thenReturn(highStarRepositories);

    // Act & Assert
    mockMvc
        .perform(get("/api/github/repositories").param("minStars", "50000"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].stargazers_count").value(65000))
        .andExpect(jsonPath("$[1].stargazers_count").value(58000))
        .andExpect(jsonPath("$[2].stargazers_count").value(200000));

    verify(gitHubRepositoryService).getFilteredRepositories(null, 50000, "stars");
  }

  @Test
  void getFilteredRepositories_WithLanguageAndMinStarsFilter_ShouldApplyBothFilters()
      throws Exception {
    // Arrange
    List<GitHubRepository> filteredRepositories =
        Arrays.asList(testRepositories.get(2), testRepositories.get(3));
    when(gitHubRepositoryService.getFilteredRepositories("Python", 50000, "stars"))
        .thenReturn(filteredRepositories);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/github/repositories").param("language", "Python").param("minStars", "50000"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].language").value("Python"))
        .andExpect(jsonPath("$[0].stargazers_count").value(65000))
        .andExpect(jsonPath("$[1].language").value("Python"))
        .andExpect(jsonPath("$[1].stargazers_count").value(58000));

    verify(gitHubRepositoryService).getFilteredRepositories("Python", 50000, "stars");
  }

  @Test
  void getFilteredRepositories_WithCustomSortParameter_ShouldPassSortToService() throws Exception {
    // Arrange
    when(gitHubRepositoryService.getFilteredRepositories(null, null, "forks"))
        .thenReturn(testRepositories);

    // Act & Assert
    mockMvc
        .perform(get("/api/github/repositories").param("sort", "forks"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(5));

    verify(gitHubRepositoryService).getFilteredRepositories(null, null, "forks");
  }

  @Test
  void getFilteredRepositories_WithAllParameters_ShouldPassAllToService() throws Exception {
    // Arrange
    List<GitHubRepository> filteredResult = Arrays.asList(testRepositories.get(0));
    when(gitHubRepositoryService.getFilteredRepositories("Java", 30000, "name"))
        .thenReturn(filteredResult);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/github/repositories")
                .param("language", "Java")
                .param("minStars", "30000")
                .param("sort", "name"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("spring-boot"));

    verify(gitHubRepositoryService).getFilteredRepositories("Java", 30000, "name");
  }

  @Test
  void getFilteredRepositories_WithInvalidMinStars_ShouldReturnInternalServerError()
      throws Exception {
    // Act & Assert
    mockMvc
        .perform(get("/api/github/repositories").param("minStars", "invalid"))
        .andExpect(status().isInternalServerError());

    verify(gitHubRepositoryService, never()).getFilteredRepositories(any(), any(), any());
  }

  @Test
  void getFilteredRepositories_WithNegativeMinStars_ShouldProcessNormally() throws Exception {
    // Arrange
    when(gitHubRepositoryService.getFilteredRepositories(null, -100, "stars"))
        .thenReturn(testRepositories);

    // Act & Assert
    mockMvc
        .perform(get("/api/github/repositories").param("minStars", "-100"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

    verify(gitHubRepositoryService).getFilteredRepositories(null, -100, "stars");
  }

  @Test
  void getFilteredRepositories_WithEmptyLanguage_ShouldTreatAsNull() throws Exception {
    // Arrange
    when(gitHubRepositoryService.getFilteredRepositories("", null, "stars"))
        .thenReturn(testRepositories);

    // Act & Assert
    mockMvc
        .perform(get("/api/github/repositories").param("language", ""))
        .andExpect(status().isOk());

    verify(gitHubRepositoryService).getFilteredRepositories("", null, "stars");
  }

  @Test
  void getFilteredRepositories_WithEmptyResult_ShouldReturnEmptyArray() throws Exception {
    // Arrange
    when(gitHubRepositoryService.getFilteredRepositories("NonExistentLanguage", 999999, "stars"))
        .thenReturn(Collections.emptyList());

    // Act & Assert
    mockMvc
        .perform(
            get("/api/github/repositories")
                .param("language", "NonExistentLanguage")
                .param("minStars", "999999"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(0));

    verify(gitHubRepositoryService).getFilteredRepositories("NonExistentLanguage", 999999, "stars");
  }

  @Test
  void getFilteredRepositories_ServiceThrowsException_ShouldReturn500() throws Exception {
    // Arrange
    when(gitHubRepositoryService.getFilteredRepositories(any(), any(), any()))
        .thenThrow(new RuntimeException("Database connection error"));

    // Act & Assert
    mockMvc.perform(get("/api/github/repositories")).andExpect(status().isInternalServerError());

    verify(gitHubRepositoryService).getFilteredRepositories(null, null, "stars");
  }

  @Test
  void getFilteredRepositories_WithSpecialCharactersInLanguage_ShouldHandleGracefully()
      throws Exception {
    // Arrange
    when(gitHubRepositoryService.getFilteredRepositories("C++", null, "stars"))
        .thenReturn(Collections.emptyList());

    // Act & Assert
    mockMvc
        .perform(get("/api/github/repositories").param("language", "C++"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(0));

    verify(gitHubRepositoryService).getFilteredRepositories("C++", null, "stars");
  }

  @Test
  void getFilteredRepositories_WithVeryLargeMinStars_ShouldProcessNormally() throws Exception {
    // Arrange
    when(gitHubRepositoryService.getFilteredRepositories(null, Integer.MAX_VALUE, "stars"))
        .thenReturn(Collections.emptyList());

    // Act & Assert
    mockMvc
        .perform(
            get("/api/github/repositories").param("minStars", String.valueOf(Integer.MAX_VALUE)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(0));

    verify(gitHubRepositoryService).getFilteredRepositories(null, Integer.MAX_VALUE, "stars");
  }

  private GitHubRepository createTestRepository(
      Long id, String name, String language, int stars, int forks) {
    return new GitHubRepository(
        id, name, "testowner", "Test repository", language, stars, forks, LocalDateTime.now());
  }
}
