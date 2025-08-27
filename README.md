# GitHub Search API

A Spring Boot application that provides a REST API for searching GitHub repositories.

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

### Start the Application

```bash
./gradlew bootRun
```

## Running Tests

```bash
./gradlew test
./gradlew integrationTest
```
