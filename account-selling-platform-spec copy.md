# Account Selling Platform - Technical Specification

## 1. ภาพรวมของระบบ (System Overview)

### 1.1 วัตถุประสงค์
สร้างเว็บไซต์ขายบัญชีออนไลน์ที่รองรับการขายบัญชีประเภทต่างๆ แบ่งเป็น 2 หมวดหลัก:
- **หมวด Account**: Google, Outlook, Hotmail, Facebook
- **หมวดเกม**: Odin Valhalla Rising, Lineage2M (แยกตาม New Account และ Gacha Account)

### 1.2 เทคโนโลยีหลัก
- **Backend**: Java Spring Boot 3.x
- **Database**: PostgreSQL 15+
- **Authentication**: JWT (Access + Refresh Token)
- **Payment**: QR Code PromptPay/SCB
- **Observability**: OpenTelemetry + Elasticsearch
- **Security**: Spring Security + BCrypt

## 2. สถาปัตยกรรมระบบ (System Architecture)

### 2.1 Layer Architecture
```
┌─────────────────────────────────────┐
│           Frontend (Web/Mobile)      │
├─────────────────────────────────────┤
│           REST API Layer            │
├─────────────────────────────────────┤
│          Service Layer              │
├─────────────────────────────────────┤
│         Repository Layer            │
├─────────────────────────────────────┤
│         PostgreSQL Database         │
└─────────────────────────────────────┘
```

### 2.2 Core Modules
- **Authentication Module**: JWT-based authentication
- **User Management Module**: User registration and profile
- **Product Management Module**: Categories, products, inventory
- **Order Management Module**: Purchase flow and order tracking
- **Payment Module**: QR Code payment integration
- **Admin Module**: Dashboard and management tools
- **Security Module**: Authorization and validation
- **Observability Module**: Logging and monitoring

## 3. โครงสร้างฐานข้อมูล (Database Schema)

### 3.1 ตารางหลัก (Core Tables)

#### 3.1.1 users
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(20) DEFAULT 'USER',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 3.1.2 categories
```sql
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL, -- 'ACCOUNT' or 'GAME'
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 3.1.3 products
```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT REFERENCES categories(id),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    server_name VARCHAR(100), -- สำหรับเกม
    account_type VARCHAR(50), -- 'NEW' or 'GACHA' สำหรับเกม
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 3.1.4 inventory
```sql
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT REFERENCES products(id),
    credentials TEXT NOT NULL, -- เก็บข้อมูล account (encrypted)
    status VARCHAR(20) DEFAULT 'AVAILABLE', -- 'AVAILABLE', 'SOLD', 'RESERVED'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sold_at TIMESTAMP
);
```

#### 3.1.5 orders
```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    product_id BIGINT REFERENCES products(id),
    inventory_id BIGINT REFERENCES inventory(id),
    order_number VARCHAR(50) UNIQUE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'PAID', 'COMPLETED', 'CANCELLED'
    payment_method VARCHAR(50),
    payment_reference VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);
```

#### 3.1.6 refresh_tokens
```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3.2 ดัชนี (Indexes)
```sql
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_status ON inventory(status);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
```

## 4. API Specification

### 4.1 Authentication APIs

#### POST /api/auth/register
```json
Request:
{
    "username": "string",
    "password": "string",
    "email": "string (optional)"
}

Response:
{
    "success": true,
    "message": "User registered successfully",
    "data": {
        "userId": 1,
        "username": "user123"
    }
}
```

#### POST /api/auth/login
```json
Request:
{
    "username": "string",
    "password": "string"
}

Response:
{
    "success": true,
    "data": {
        "accessToken": "jwt_token",
        "refreshToken": "refresh_token",
        "expiresIn": 3600,
        "user": {
            "id": 1,
            "username": "user123",
            "role": "USER"
        }
    }
}
```

### 4.2 Product APIs

#### GET /api/products
```json
Query Parameters:
- category: string (optional)
- server: string (optional)
- type: string (optional)
- page: int (default: 0)
- size: int (default: 20)

Response:
{
    "success": true,
    "data": {
        "content": [
            {
                "id": 1,
                "name": "Google Account - New",
                "description": "Fresh Google account",
                "price": 50.00,
                "category": "Account",
                "server": null,
                "accountType": null,
                "imageUrl": "https://...",
                "stockCount": 15
            }
        ],
        "totalElements": 100,
        "totalPages": 5,
        "currentPage": 0
    }
}
```

### 4.3 Order APIs

#### POST /api/orders
```json
Request:
{
    "productId": 1,
    "paymentMethod": "QR_CODE"
}

Response:
{
    "success": true,
    "data": {
        "orderId": 1,
        "orderNumber": "ORD-20250718-001",
        "amount": 50.00,
        "qrCodeUrl": "data:image/png;base64,iVBOR...",
        "paymentReference": "REF123456",
        "expiresAt": "2025-07-18T15:30:00Z"
    }
}
```

## 5. Security Requirements

### 5.1 Authentication & Authorization
- JWT-based authentication with access and refresh tokens
- Role-based access control (USER, ADMIN)
- Password hashing using BCrypt
- Token expiration and refresh mechanism

### 5.2 API Security
- CORS configuration for allowed origins
- CSRF protection for state-changing operations
- Rate limiting (100 requests per minute per IP)
- Input validation and sanitization
- SQL injection prevention through JPA

### 5.3 Data Protection
- Sensitive data encryption (account credentials)
- HTTPS enforcement
- Secure headers configuration
- Database connection encryption

## 6. Performance Requirements

### 6.1 Response Time
- API response time < 200ms (95th percentile)
- Database query optimization
- Connection pooling configuration
- Caching for frequently accessed data

### 6.2 Scalability
- Support for 1000+ concurrent users
- Horizontal scaling capability
- Database connection pooling
- Stateless application design

## 7. Observability & Monitoring

### 7.1 OpenTelemetry Integration
- Distributed tracing for all HTTP requests
- Custom spans for business operations
- Metrics collection for performance monitoring
- Integration with Jaeger for trace visualization

### 7.2 Logging Strategy
- Structured logging with JSON format
- Log levels: ERROR, WARN, INFO, DEBUG
- Elasticsearch integration for log aggregation
- Security event logging (login attempts, failures)

### 7.3 Health Checks
- Application health endpoint
- Database connectivity check
- External service dependency checks
- Kubernetes readiness and liveness probes

## 8. Deployment Architecture

### 8.1 Environment Configuration
- Development, Staging, Production environments
- Environment-specific configuration files
- Secret management for sensitive data
- Database migration scripts

### 8.2 Infrastructure Requirements
- Java 17+ runtime environment
- PostgreSQL 15+ database
- Redis for session storage (optional)
- Load balancer for high availability
- SSL/TLS certificates

## 9. Testing Strategy

### 9.1 Unit Testing
- Service layer unit tests (80%+ coverage)
- Repository layer integration tests
- Security configuration tests
- Mock external dependencies

### 9.2 Integration Testing
- API endpoint testing
- Database integration tests
- Authentication flow tests
- Payment integration tests

### 9.3 Performance Testing
- Load testing for concurrent users
- Database performance testing
- API response time validation
- Memory and CPU usage monitoring

## 10. Implementation Phases

### Phase 1: Core Foundation (Week 1-2)
- Project setup and configuration
- Database schema creation
- Basic authentication system
- User management APIs

### Phase 2: Product Management (Week 3-4)
- Category and product management
- Inventory system
- Product search and filtering
- Admin product management

### Phase 3: Order & Payment (Week 5-6)
- Order creation and management
- QR Code payment integration
- Order status tracking
- Account delivery system

### Phase 4: Admin Dashboard (Week 7-8)
- Admin authentication
- Sales dashboard
- Inventory management
- System monitoring

### Phase 5: Security & Observability (Week 9-10)
- Security hardening
- OpenTelemetry integration
- Elasticsearch logging
- Performance optimization

## 11. Risk Assessment

### 11.1 Technical Risks
- Payment gateway integration complexity
- Concurrent purchase handling
- Data security and encryption
- Performance under high load

### 11.2 Mitigation Strategies
- Thorough testing of payment flows
- Database transaction management
- Security audit and penetration testing
- Load testing and performance tuning

## 12. Success Criteria

### 12.1 Functional Requirements
- ✅ User registration and authentication
- ✅ Product browsing and search
- ✅ Secure purchase flow
- ✅ Account delivery system
- ✅ Admin management tools

### 12.2 Non-Functional Requirements
- ✅ 99.9% uptime availability
- ✅ < 200ms API response time
- ✅ Support 1000+ concurrent users
- ✅ Zero data breaches
- ✅ Complete audit trail

---

*Document Version: 1.0*  
*Last Updated: July 18, 2025*  
*Author: Senior Software Architect*