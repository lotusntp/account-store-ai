# Design Document: Account Selling Platform

## Overview

The Account Selling Platform is a web application built with Spring Boot that enables the online selling of various digital accounts and game-related products. The system provides a secure marketplace for customers to browse, purchase, and download digital accounts across different categories, while offering comprehensive admin functionality for inventory management, sales tracking, and system monitoring.

This design document outlines the technical architecture, components, interfaces, data models, and other essential aspects of the system implementation.

## Architecture

### System Architecture

The Account Selling Platform follows a layered architecture pattern with clear separation of concerns:

- **Presentation Layer**: REST API endpoints that handle HTTP requests and responses
- **Service Layer**: Business logic implementation
- **Repository Layer**: Data access and persistence
- **Database Layer**: PostgreSQL database for storing application data
- **External Services**: Integration with payment gateways and observability tools

### Technology Stack

- **Backend Framework**: Spring Boot 3.x
  - Spring Security
  - Spring Data JPA
  - Spring Validation
  - Spring Web
- **Database**: PostgreSQL
- **Authentication**: JWT (JSON Web Tokens)
- **API Documentation**: SpringDoc OpenAPI (Swagger)
- **Payment Integration**: QR Code Payment Gateway (SCB/??????)
- **Observability**: OpenTelemetry, Elasticsearch, Grafana, Jaeger
- **Testing**: JUnit 5, Mockito, TestContainers
- **Build Tool**: Maven
- **Containerization**: Docker

### Deployment Architecture

The system will be deployed using Docker containers for easy scaling and management:

- **Application Servers**: Multiple instances behind a load balancer
- **Database**: PostgreSQL with replication for high availability
- **Cache**: Redis for session management and frequently accessed data
- **Observability Stack**: Elasticsearch, Grafana, and Jaeger for monitoring and tracing

## Components and Interfaces

### Core Components

1. **User Management Module**
   - User registration and authentication
   - Profile management
   - Order history tracking
   - Role-based access control

2. **Product Catalog Module**
   - Category management
   - Product listing and details
   - Search and filtering
   - Inventory management

3. **Payment Processing Module**
   - QR code generation
   - Payment status tracking
   - Order fulfillment
   - Transaction history

4. **Admin Dashboard Module**
   - Sales analytics
   - Inventory management
   - User management
   - System configuration

5. **Security Module**
   - Authentication and authorization
   - Input validation
   - Rate limiting
   - CORS and CSRF protection

6. **Observability Module**
   - Request tracing
   - Logging
   - Metrics collection
   - Alerting

### API Interfaces

#### User Management API

| Endpoint | Method | Description |
|---------|--------|-------------|
| /api/auth/register | POST | Register a new user |
| /api/auth/login | POST | Authenticate user and get tokens |
| /api/auth/refresh | POST | Refresh access token |
| /api/auth/logout | POST | Invalidate tokens |
| /api/users/profile | GET | Get user profile |
| /api/users/orders | GET | Get user order history |
| /api/users/download/{orderId} | GET | Download purchased account credentials as .txt file |

#### Product API

| Endpoint | Method | Description |
|---------|--------|-------------|
| /api/categories | GET | Get all categories |
| /api/categories/{id} | GET | Get category by ID |
| /api/products | GET | Get all products (with filtering) |
| /api/products/{id} | GET | Get product by ID |
| /api/products/search | GET | Search products |

#### Payment API

| Endpoint | Method | Description |
|---------|--------|-------------|
| /api/payments/generate | POST | Generate payment QR code |
| /api/payments/status/{id} | GET | Check payment status |
| /api/payments/webhook | POST | Payment gateway webhook |

#### Admin API

| Endpoint | Method | Description |
|---------|--------|-------------|
| /api/admin/categories | POST, PUT, DELETE | Manage categories |
| /api/admin/products | POST, PUT, DELETE | Manage products |
| /api/admin/stock | POST, PUT | Manage product stock |
| /api/admin/users | GET, PUT | Manage users |
| /api/admin/orders | GET | View orders |
| /api/admin/dashboard | GET | Get dashboard data |

## Data Models

### Entity Relationship Diagram

The following entities and their relationships form the core data model of the system:

- **User**: Stores user account information
- **Role**: Defines user roles and permissions
- **Category**: Represents product categories
- **Product**: Contains product information
- **Stock**: Stores account credentials for products
- **Order**: Records customer purchases
- **OrderItem**: Links orders to purchased accounts
- **Payment**: Tracks payment information

### Key Entities

#### User

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Primary key |
| username | String | Unique username |
| password | String | Hashed password |
| email | String | User email (optional) |
| createdAt | Timestamp | Account creation time |
| updatedAt | Timestamp | Last update time |
| roles | Set<Role> | User roles |
| orders | Set<Order> | User orders |

#### Role

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| name | String | Role name (USER, ADMIN) |

#### Category

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Primary key |
| name | String | Category name |
| description | String | Category description |
| parentCategory | Category | Parent category (for hierarchy) |
| subCategories | Set<Category> | Child categories |
| products | Set<Product> | Products in this category |

#### Product

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Primary key |
| name | String | Product name |
| description | String | Product description |
| price | BigDecimal | Product price |
| imageUrl | String | Product image URL |
| server | String | Game server (if applicable) |
| category | Category | Product category |
| active | Boolean | Product availability status |
| createdAt | Timestamp | Creation time |
| updatedAt | Timestamp | Last update time |
| stockItems | Set<Stock> | Available stock items |

#### Stock

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Primary key |
| product | Product | Associated product |
| credentials | String | Account credentials (encrypted) |
| sold | Boolean | Whether item is sold |
| reservedUntil | Timestamp | Reservation expiry time |
| createdAt | Timestamp | Creation time |

#### Order

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Primary key |
| user | User | Customer who placed the order |
| totalAmount | BigDecimal | Total order amount |
| status | Enum | Order status (PENDING, COMPLETED, FAILED) |
| createdAt | Timestamp | Order creation time |
| updatedAt | Timestamp | Last update time |
| orderItems | Set<OrderItem> | Items in this order |
| payment | Payment | Associated payment |

#### OrderItem

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Primary key |
| order | Order | Associated order |
| product | Product | Purchased product |
| stockItem | Stock | Specific stock item purchased |
| price | BigDecimal | Price at time of purchase |

#### Payment

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Primary key |
| order | Order | Associated order |
| amount | BigDecimal | Payment amount |
| status | Enum | Payment status (PENDING, COMPLETED, FAILED) |
| paymentMethod | String | Payment method used |
| transactionId | String | External payment reference |
| qrCodeUrl | String | URL to payment QR code |
| createdAt | Timestamp | Payment creation time |
| updatedAt | Timestamp | Last update time |

## Error Handling

### Exception Hierarchy

- **BaseException**: Abstract base exception for all application exceptions
  - **AuthenticationException**: Authentication-related exceptions
    - **InvalidCredentialsException**: Invalid username or password
    - **TokenExpiredException**: JWT token has expired
    - **InvalidTokenException**: JWT token is invalid
  - **AuthorizationException**: Authorization-related exceptions
    - **InsufficientPermissionsException**: User lacks required permissions
  - **ResourceException**: Resource-related exceptions
    - **ResourceNotFoundException**: Requested resource not found
    - **ResourceAlreadyExistsException**: Resource already exists
    - **ResourceInvalidException**: Resource validation failed
  - **PaymentException**: Payment-related exceptions
    - **PaymentFailedException**: Payment processing failed
    - **PaymentTimeoutException**: Payment timed out
  - **StockException**: Stock-related exceptions
    - **OutOfStockException**: Product is out of stock
    - **StockReservationException**: Failed to reserve stock

### Global Exception Handling

The application will implement a global exception handler using Spring''s @ControllerAdvice to handle exceptions consistently across the application:

- Convert exceptions to appropriate HTTP status codes
- Provide standardized error response format
- Log exceptions with appropriate severity levels
- Include relevant error details while avoiding sensitive information exposure

### Error Response Format

The API will return standardized error responses in a JSON format that includes:

- HTTP status code
- Error type
- Error message
- Timestamp
- Request path
- Detailed error information (when applicable)

Example JSON error response:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid input data",
  "timestamp": "2025-07-18T15:30:45.123Z",
  "path": "/api/products",
  "details": [
    "Product name is required",
    "Price must be greater than zero"
  ]
}
```

## Testing Strategy

### Testing Levels

1. **Unit Testing**
   - Test individual components in isolation
   - Mock dependencies using Mockito
   - Focus on business logic and edge cases
   - Aim for high code coverage (>80%)

2. **Integration Testing**
   - Test interactions between components
   - Use TestContainers for database integration tests
   - Test API endpoints with MockMvc
   - Verify database operations

3. **System Testing**
   - End-to-end testing of complete workflows
   - Test critical user journeys
   - Verify integration with external systems using WireMock

### Test Categories

1. **Functional Tests**
   - Verify system behavior against requirements
   - Test positive and negative scenarios
   - Validate business rules

2. **Security Tests**
   - Authentication and authorization
   - Input validation and sanitization
   - Protection against common vulnerabilities (OWASP Top 10)

3. **Performance Tests**
   - Load testing with JMeter
   - Stress testing critical endpoints
   - Database query optimization

4. **Concurrency Tests**
   - Test simultaneous user actions
   - Verify data consistency under concurrent access
   - Test race conditions in critical operations (e.g., stock reservation)

## Security Implementation

### Authentication and Authorization

1. **JWT-based Authentication**
   - Short-lived access tokens (15-30 minutes)
   - Longer-lived refresh tokens (7 days)
   - Secure token storage and transmission
   - Token blacklisting for logout

2. **Role-based Access Control**
   - User roles: USER, ADMIN
   - Method-level security with Spring Security annotations
   - URL-based security for API endpoints

### Data Protection

1. **Password Security**
   - BCrypt password hashing with appropriate work factor
   - Password strength validation

2. **Sensitive Data Encryption**
   - Encrypt account credentials in the database
   - Use AES-256 encryption for sensitive data

3. **API Security**
   - Input validation for all API endpoints
   - Rate limiting to prevent abuse
   - HTTPS for all communications
   - Proper CORS configuration
   - CSRF protection for non-GET requests
   - Proper locking mechanisms for concurrent transactions

## Observability Implementation

### Logging

- Structured logging with SLF4J and Logback
- Log levels: ERROR, WARN, INFO, DEBUG, TRACE
- Include request IDs for correlation
- Ship logs to Elasticsearch for centralized storage and analysis
- Capture detailed context for troubleshooting when errors occur

### Metrics

- Application metrics with Micrometer
- JVM metrics (memory, CPU, garbage collection)
- Business metrics (orders, payments, user registrations)
- Custom metrics for critical operations
- Performance monitoring to maintain acceptable parameters under increased load

### Tracing

- Distributed tracing with OpenTelemetry
- Trace HTTP requests across services
- Trace database operations
- Visualize traces with Jaeger
- Provide correlated logs and traces for incident investigation

### Alerting

- Define alert thresholds for critical metrics
- Set up notifications for system errors
- Monitor resource utilization
- Alert on business anomalies (e.g., sudden drop in orders)
- Trigger alerts when performance metrics change significantly

## Conclusion

This design document provides a comprehensive blueprint for implementing the Account Selling Platform using Spring Boot. The architecture and design decisions are aligned with the requirements and aim to create a secure, scalable, and maintainable system. The implementation will follow best practices for Java and Spring Boot development, with a focus on security, performance, and observability.
