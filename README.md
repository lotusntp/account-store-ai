# Account Selling Platform

A Spring Boot application for selling digital accounts and game-related products.

## Features

- User registration and authentication
- Product catalog management
- Secure payment processing
- Admin dashboard
- Comprehensive monitoring and logging

## Technology Stack

- Spring Boot 3.x
- Spring Security with JWT
- Spring Data JPA
- PostgreSQL
- OpenTelemetry for observability
- Elasticsearch for logging
- Docker for containerization

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven
- PostgreSQL
- Docker (optional)

### Setup

1. Clone the repository
2. Configure database connection in `application.properties`
3. Run the application:

```bash
mvn spring-boot:run
```

### Building for Production

```bash
mvn clean package
```

## API Documentation

API documentation is available at `/swagger-ui.html` when the application is running.

## Testing

```bash
mvn test
```