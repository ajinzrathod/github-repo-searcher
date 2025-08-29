package com.ajinz.githubsearch.repository;

import com.ajinz.githubsearch.dto.github.GitHubRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GitHubRepositoryRepository extends JpaRepository<GitHubRepository, Long> {
  Optional<GitHubRepository> findByGithubRepoId(Long gitHubRepoId);

  @Query(
      "SELECT r FROM GitHubRepository r WHERE "
          + "(:language IS NULL OR r.programmingLanguage = :language) AND "
          + "(:minStars IS NULL OR r.starsCount >= :minStars) "
          + "ORDER BY "
          + "CASE WHEN :sortBy = 'stars' THEN r.starsCount END DESC, "
          + "CASE WHEN :sortBy = 'forks' THEN r.forksCount END DESC, "
          + "CASE WHEN :sortBy = 'updated' THEN r.gitRepoLastUpdatedDate END DESC, "
          + "CASE WHEN :sortBy = 'name' THEN r.repoName END ASC, "
          + "r.starsCount DESC")
  List<GitHubRepository> findRepositoriesWithFilters(
      @Param("language") String language,
      @Param("minStars") Integer minStars,
      @Param("sortBy") String sortBy);
}
