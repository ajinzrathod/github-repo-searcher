# GitHub Search API

A Spring Boot application that provides a REST API for searching GitHub repositories with PostgreSQL database storage.

## API Endpoints

### Health Check

```
GET /api/github/health
```

Returns application health status.

### Search Repositories (POST)

```
POST /api/github/search/repositories
Content-Type: application/json
```

Search GitHub repositories using a POST request with JSON body.

**Request Body:**
```json
{
  "query": "spring boot",
  "language": "Java",
  "sort": "stars",
  "order": "desc",
  "page": 1,
  "perPage": 10
}
```

**Parameters:**

- `query` (required): Search query string
- `language` (optional): Programming language filter
- `sort` (optional): Sort by `stars`, `forks`, or `updated` (default: `stars`)
- `order` (optional): Sort order `asc` or `desc` (default: `desc`)
- `page` (optional): Page number for pagination (default: 1, max: 34)
- `perPage` (optional): Number of results per page (default: 10, max: 100)

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/github/search/repositories \
  -H "Content-Type: application/json" \
  -d '{
    "query": "spring boot",
    "language": "Java",
    "sort": "stars"
  }'
```

### Get Saved Repositories (GET)

```
GET /api/github/repositories
```

Retrieve previously saved repositories from the database with optional filtering and sorting.

**Query Parameters:**

- `language` (optional): Filter repositories by programming language (e.g., `Java`, `Python`, `JavaScript`)
- `minStars` (optional): Filter repositories with minimum star count (e.g., `1000`)
- `sort` (optional): Sort results by `stars`, `forks`, `updated`, or `name` (default: `stars`)

**Example Requests:**

```bash
# Get all saved repositories
curl http://localhost:8080/api/github/repositories

# Filter by programming language
curl http://localhost:8080/api/github/repositories?language=Java

# Filter by minimum stars
curl http://localhost:8080/api/github/repositories?minStars=5000

# Combine filters and custom sorting
curl http://localhost:8080/api/github/repositories?language=Python&minStars=1000&sort=forks

# Sort by repository name
curl http://localhost:8080/api/github/repositories?sort=name
```

**Key Features:**

- Returns repositories previously saved from GitHub API searches
- Supports filtering by programming language and minimum star count
- Flexible sorting options (stars, forks, last updated, name)
- Results are sorted by star count in descending order by default
- Returns empty array if no repositories match the filters

## Architecture & Design Patterns

### 1. **Layered Architecture**

- **Controller Layer**: Handles HTTP requests and responses
- **Service Layer**: Contains business logic and external API integration
- **DTO Layer**: Data transfer objects for clean API contracts

### 2. **Design Patterns Used**

- **Dependency Injection**: Spring's IoC container
- **Builder Pattern**: WebClient configuration
- **Record Pattern**: Immutable DTOs using Java records

### 3. **Best Practices Implemented**

- **Reactive Programming**: Non-blocking I/O with WebClient
- **Input Validation**: Jakarta validation annotations
- **Error Handling**: Global exception handler with proper HTTP status codes
- **Logging**: Structured logging with SLF4J
- **Configuration Management**: Externalized configuration properties
- **Testing**: Integration tests with MockMvc

## Running the Application

### Prerequisites

- Java 21
- Gradle 8.x
- Docker and Docker Compose (for PostgreSQL)

### Database Setup

This application uses PostgreSQL as the database. The easiest way to run it is using Docker Compose.

#### 1. Environment Variables

Create a `.env` file in the project root with the following variables:

```env
POSTGRES_DB=postgres
POSTGRES_USER=githubsearch_user
POSTGRES_PASSWORD=your_secure_password_here
```

#### 2. Start PostgreSQL

```bash
docker-compose up -d postgres
```

This will start PostgreSQL on port 5432. The database will be created automatically with the credentials from your
`.env` file.

#### 3. Verify Database Connection

You can verify PostgreSQL is running:

```bash
docker ps
```

You should see the `githubsearch-postgres` container running.

### Start the Application

```bash
# This will start both PostgreSQL and the application
docker-compose up
```

### Application URLs

- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/health
- **PostgreSQL**: localhost:5432

### Database Schema

The application automatically creates the required database tables using Flyway migrations on startup. The schema
includes:

- Repository details storage (ID, name, description, owner, language, stars, forks, etc.)
- Full GitHub API response storage for complete data preservation
- Optimized indexes for filtering and sorting operations

### Stopping the Application

```bash
# Stop all services
docker-compose down
```

## Running Tests

```bash
./gradlew bootRun # required if any DB migrations are pending
./gradlew test
./gradlew integrationTest
```
