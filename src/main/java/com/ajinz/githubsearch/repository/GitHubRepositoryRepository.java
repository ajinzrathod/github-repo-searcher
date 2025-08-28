package com.ajinz.githubsearch.repository;

import com.ajinz.githubsearch.dto.github.GitHubRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GitHubRepositoryRepository extends JpaRepository<GitHubRepository, Long> {
  Optional<GitHubRepository> findByGithubRepoId(Long gitHubRepoId);
}
