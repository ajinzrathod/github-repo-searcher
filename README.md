# GitHub Repository Search API

A Spring Boot application that provides a REST API for searching GitHub repositories using the GitHub Search API.

## Features

- **RESTful API** with both GET and POST endpoints
- **Reactive programming** using Spring WebFlux
- **Input validation** with comprehensive error handling
- **Clean architecture** with service layer pattern
- **Comprehensive logging** for debugging and monitoring
- **Integration tests** for API validation

## API Endpoints

### Health Check
```
GET /api/github/health
```
Returns application health status.

### Search Repositories (GET)
```
GET /api/github/search/repositories?query={query}&language={language}&sort={sort}&order={order}&page={page}&per_page={perPage}
```

**Parameters:**
- `query` (required): Search query string
- `language` (optional): Programming language filter
- `sort` (optional): Sort by `stars`, `forks`, or `updated` (default: `stars`)
- `order` (optional): Sort order `asc` or `desc` (default: `desc`)
- `page` (optional): Page number 1-34 (default: `1`)
- `per_page` (optional): Results per page 1-100 (default: `10`)

**Example:**
```bash
curl "http://localhost:8080/api/github/search/repositories?query=spring&language=java&sort=stars&order=desc&per_page=5"
```

### Search Repositories (POST)
```
POST /api/github/search/repositories
Content-Type: application/json
```

**Request Body:**
```json
{
  "query": "spring",
  "language": "java",
  "sort": "stars",
  "order": "desc",
  "page": 1,
  "perPage": 5
}
```

**Example:**
```bash
curl -X POST "http://localhost:8080/api/github/search/repositories" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "spring",
    "language": "java",
    "sort": "stars",
    "order": "desc",
    "perPage": 5
  }'
```

## Response Format

```json
{
  "total_count": 1234567,
  "incomplete_results": false,
  "items": [
    {
      "id": 6296790,
      "name": "spring-boot",
      "full_name": "spring-projects/spring-boot",
      "description": "Spring Boot helps you to create Spring-powered, production-grade applications...",
      "html_url": "https://github.com/spring-projects/spring-boot",
      "clone_url": "https://github.com/spring-projects/spring-boot.git",
      "language": "Java",
      "stargazers_count": 78184,
      "watchers_count": 78184,
      "forks_count": 41424,
      "open_issues_count": 578,
      "created_at": "2012-10-19T15:02:57",
      "updated_at": "2025-08-24T08:45:32",
      "pushed_at": "2025-08-23T06:53:31",
      "owner": {
        "id": 317776,
        "login": "spring-projects",
        "avatar_url": "https://avatars.githubusercontent.com/u/317776?v=4",
        "html_url": "https://github.com/spring-projects",
        "type": "Organization"
      }
    }
  ]
}
```

## Architecture & Design Patterns

### 1. **Layered Architecture**
- **Controller Layer**: Handles HTTP requests and responses
- **Service Layer**: Contains business logic and external API integration
- **DTO Layer**: Data transfer objects for clean API contracts

### 2. **Design Patterns Used**
- **Dependency Injection**: Spring's IoC container
- **Interface Segregation**: Service interfaces separate from implementation
- **Builder Pattern**: WebClient configuration
- **Record Pattern**: Immutable DTOs using Java records
- **Factory Pattern**: WebClient.Builder bean configuration

### 3. **Best Practices Implemented**
- **Reactive Programming**: Non-blocking I/O with WebClient
- **Input Validation**: Jakarta validation annotations
- **Error Handling**: Global exception handler with proper HTTP status codes
- **Logging**: Structured logging with SLF4J
- **Configuration Management**: Externalized configuration properties
- **Testing**: Integration tests with MockMvc

## Project Structure

```
src/
├── main/java/com/ajinz/githubsearch/
│   ├── config/
│   │   ├── GlobalExceptionHandler.java
│   │   └── WebClientConfig.java
│   ├── controller/
│   │   └── SearchController.java
│   ├── dto/
│   │   ├── ApiErrorResponse.java
│   │   ├── GitHubOwner.java
│   │   ├── GitHubRepository.java
│   │   ├── GitHubSearchResponse.java
│   │   └── SearchRequest.java
│   ├── service/
│   │   ├── GitHubSearchService.java
│   │   └── impl/
│   │       └── GitHubSearchServiceImpl.java
│   └── GithubsearchApplication.java
└── integration-test/java/
    └── com/ajinz/githubsearch/controller/
        └── SearchControllerIntegrationTest.java
```

## Running the Application

### Prerequisites
- Java 21
- Gradle 8.x

### Start the Application
```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### Run Tests
```bash
./gradlew test
./gradlew integrationTest
```

## Configuration

The application can be configured via `application.properties`:

```properties
# GitHub API Configuration
github.api.base-url=https://api.github.com
github.api.version=2022-11-28

# Logging configuration
logging.level.com.ajinz.githubsearch=DEBUG
```

## Error Handling

The API provides comprehensive error responses:

### Validation Error (400)
```json
{
  "message": "Validation failed",
  "error": "VALIDATION_ERROR",
  "status": 400,
  "timestamp": "2025-08-24T12:30:45",
  "path": "/api/github/search/repositories",
  "validationErrors": {
    "query": "Query cannot be blank",
    "page": "Page must be at least 1"
  }
}
```

### Internal Server Error (500)
```json
{
  "message": "Failed to search repositories",
  "error": "INTERNAL_ERROR",
  "status": 500,
  "timestamp": "2025-08-24T12:30:45",
  "path": "/api/github/search/repositories"
}
```

## Dependencies

- Spring Boot 3.5.4
- Spring WebFlux (Reactive Web)
- Spring Validation
- Jackson (JSON processing)
- SLF4J (Logging)
- JUnit 5 (Testing)
