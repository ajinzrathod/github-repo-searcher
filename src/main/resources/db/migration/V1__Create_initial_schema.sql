-- Initial schema creation for GitHub responses
CREATE TABLE IF NOT EXISTS responses (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    -- GitHub repository specific fields
    github_repo_id BIGINT,
    repo_name VARCHAR(255),
    description TEXT,
    owner_name VARCHAR(255),
    programming_language VARCHAR(100),
    stars_count INTEGER DEFAULT 0,
    forks_count INTEGER DEFAULT 0,
    last_updated_date TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes only for required filtering and sorting operations
CREATE INDEX idx_responses_programming_language ON responses(programming_language);
CREATE INDEX idx_responses_stars_count ON responses(stars_count);
CREATE INDEX idx_responses_forks_count ON responses(forks_count);
CREATE INDEX idx_responses_last_updated_date ON responses(last_updated_date);
