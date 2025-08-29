package com.ajinz.githubsearch.service;

import com.ajinz.githubsearch.dto.github.GitHubRepository;
import com.ajinz.githubsearch.repository.GitHubRepositoryRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GitHubRepositoryService {
  GitHubRepositoryRepository gitHubRepositoryRepository;

  public GitHubRepositoryService(GitHubRepositoryRepository gitHubRepositoryRepository) {
    this.gitHubRepositoryRepository = gitHubRepositoryRepository;
  }

  @Transactional
  public void saveAllGitHubRepositories(List<GitHubRepository> gitHubRepositories) {
    List<GitHubRepository> repositoriesToAdd =
        gitHubRepositories.stream()
            .filter(
                gitHubRepository ->
                    gitHubRepositoryRepository
                        .findByGithubRepoId(gitHubRepository.getGithubRepoId())
                        .isEmpty())
            .toList();
    gitHubRepositoryRepository.saveAll(repositoriesToAdd);
  }

  public List<GitHubRepository> getAllSavedRepositories() {
    return gitHubRepositoryRepository.findAll();
  }

  public List<GitHubRepository> getFilteredRepositories(
      String language, Integer minStars, String sortBy) {
    if (sortBy == null || sortBy.isEmpty()) {
      sortBy = "stars";
    }

    return gitHubRepositoryRepository.findRepositoriesWithFilters(language, minStars, sortBy);
  }
}
