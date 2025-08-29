package com.ajinz.githubsearch.dto.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "github_repository")
public class GitHubRepository {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonProperty("id")
  @Column(name = "github_repo_id")
  private Long githubRepoId;

  @JsonProperty("name")
  @Column(name = "repo_name", length = 255)
  private String repoName;

  @JsonProperty("description")
  @Column(columnDefinition = "TEXT")
  private String description;

  @JsonProperty("owner")
  @Column(name = "owner_name", length = 255)
  private String ownerName;

  @JsonProperty("language")
  @Column(name = "programming_language", length = 100)
  private String programmingLanguage;

  @JsonProperty("stargazers_count")
  @Column(name = "stars_count")
  private Integer starsCount = 0;

  @JsonProperty("forks_count")
  @Column(name = "forks_count")
  private Integer forksCount = 0;

  @JsonProperty("updated_at")
  @Column(name = "git_repo_last_updated_date")
  private LocalDateTime gitRepoLastUpdatedDate;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at")
  private LocalDateTime updatedAt = LocalDateTime.now();

  @JsonProperty("full_url")
  @Transient
  private LocalDateTime full_url;

  public GitHubRepository() {}

  // Constructor for testing purposes
  public GitHubRepository(
      Long githubRepoId,
      String repoName,
      String ownerName,
      String description,
      String programmingLanguage,
      Integer starsCount,
      Integer forksCount,
      LocalDateTime gitRepoLastUpdatedDate) {
    this.githubRepoId = githubRepoId;
    this.repoName = repoName;
    this.ownerName = ownerName;
    this.description = description;
    this.programmingLanguage = programmingLanguage;
    this.starsCount = starsCount;
    this.forksCount = forksCount;
    this.gitRepoLastUpdatedDate = gitRepoLastUpdatedDate;
  }

  // Simplified constructor for basic test cases
  public GitHubRepository(Long githubRepoId, String repoName, String ownerName) {
    this(
        githubRepoId,
        repoName,
        ownerName,
        "Test repository description",
        "Java",
        100,
        50,
        LocalDateTime.now());
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  @JsonProperty("owner")
  public void setOwner(java.util.Map<String, Object> owner) {
    if (owner != null) {
      this.ownerName = (String) owner.get("login");
    }
  }

  @JsonProperty("full_url")
  public String setFullUrl() {
    return "https://github.com/" + this.ownerName + "/" + this.repoName;
  }

  public Long getGithubRepoId() {
    return githubRepoId;
  }

  public String getRepoName() {
    return repoName;
  }

  public String getDescription() {
    return description;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public String getProgrammingLanguage() {
    return programmingLanguage;
  }

  public Integer getStarsCount() {
    return starsCount;
  }

  public Integer getForksCount() {
    return forksCount;
  }

  public LocalDateTime getGitRepoLastUpdatedDate() {
    return gitRepoLastUpdatedDate;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
