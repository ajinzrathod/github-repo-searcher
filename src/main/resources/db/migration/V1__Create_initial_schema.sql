-- Initial schema creation for GitHub responses
CREATE TABLE IF NOT EXISTS github_repository (
    id BIGSERIAL PRIMARY KEY,
    -- GitHub repository specific fields
    github_repo_id BIGINT,
    repo_name VARCHAR(255),
    description TEXT,
    owner_name VARCHAR(255),
    programming_language VARCHAR(100),
    stars_count INTEGER DEFAULT 0,
    forks_count INTEGER DEFAULT 0,
    git_repo_last_updated_date TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes only for required filtering and sorting operations
CREATE INDEX idx_github_repository_programming_language ON github_repository(programming_language);
CREATE INDEX idx_github_repository_stars_count ON github_repository(stars_count);
CREATE INDEX idx_github_repository_forks_count ON github_repository(forks_count);
CREATE INDEX idx_github_repository_last_updated_date ON github_repository(git_repo_last_updated_date);
