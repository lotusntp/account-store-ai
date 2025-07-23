# Account Selling Platform - Project Overview

## ภาพรวมโครงการ (Project Overview)

Account Selling Platform เป็นเว็บแอปพลิเคชันที่พัฒนาด้วย Spring Boot สำหรับการขายบัญชีดิจิทัลและผลิตภัณฑ์ที่เกี่ยวข้องกับเกมออนไลน์ ระบบนี้ให้บริการตลาดกลางที่ปลอดภัยสำหรับลูกค้าในการเรียกดู ซื้อ และดาวน์โหลดบัญชีดิจิทัลในหมวดหมู่ต่างๆ พร้อมด้วยฟังก์ชันการจัดการแบบครอบคลุมสำหรับผู้ดูแลระบบ

## เทคโนโลยีที่ใช้ (Technology Stack)

### Backend Framework

- **Java 17**
- **Spring Boot 3.2.0** พร้อมด้วย:
  - Spring Security (สำหรับการรักษาความปลอดภัย)
  - Spring Data JPA (สำหรับการจัดการข้อมูล)
  - Spring Validation (สำหรับการตรวจสอบข้อมูล)
  - Spring Web (สำหรับ REST API)
  - Spring Actuator (สำหรับ monitoring)

### Database & Storage

- **PostgreSQL** - ฐานข้อมูลหลักพร้อม Flyway migrations
- **H2** - ฐานข้อมูลในหน่วยความจำสำหรับการทดสอบ
- **HikariCP** - connection pooling

### Security & Authentication

- **JWT (JSON Web Tokens)** - สำหรับการยืนยันตัวตนพร้อม refresh tokens
- **BCrypt** - สำหรับการเข้ารหัสรหัสผ่าน
- **Bucket4j** - สำหรับ rate limiting

### Observability & Monitoring

- **OpenTelemetry** - สำหรับ distributed tracing
- **Micrometer Tracing** - auto-instrumentation
- **Elasticsearch** - สำหรับเก็บ structured logs
- **Prometheus** - metrics endpoint

### Testing & Build

- **JUnit 5** และ **Mockito** - สำหรับ unit testing
- **TestContainers** - สำหรับ integration testing
- **Maven** - build tool

## สถาปัตยกรรมระบบ (System Architecture)

### Layered Architecture Pattern

1. **Presentation Layer** - REST API endpoints
2. **Service Layer** - Business logic implementation (กำลังพัฒนา)
3. **Repository Layer** - Data access และ persistence
4. **Database Layer** - PostgreSQL database

### Core Components

1. **User Management Module** - จัดการผู้ใช้และการยืนยันตัวตน
2. **Product Catalog Module** - จัดการหมวดหมู่และรายการสินค้า
3. **Payment Processing Module** - ประมวลผลการชำระเงิน
4. **Admin Dashboard Module** - แดชบอร์ดสำหรับผู้ดูแลระบบ
5. **Security Module** - ระบบรักษาความปลอดภัย
6. **Observability Module** - การติดตามและ monitoring

## คำสั่งการพัฒนา (Build and Development Commands)

### การรันแอปพลิเคชัน

```bash
# รันด้วย Maven
mvn spring-boot:run

# รันด้วย profile เฉพาะ
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Build JAR file
mvn clean package

# รัน JAR file ที่ build แล้ว
java -jar target/platform-0.0.1-SNAPSHOT.jar
```

### การทดสอบ

```bash
# รัน tests ทั้งหมด
mvn test

# รัน test class เฉพาะ
mvn test -Dtest=UserRepositoryTest

# รัน integration tests
mvn test -Dtest=*IntegrationTest

# รัน tests ด้วย profile เฉพาะ
mvn test -Dspring.profiles.active=test
```

### การจัดการฐานข้อมูล

```bash
# รัน Flyway migrations
mvn flyway:migrate

# ตรวจสอบสถานะ migration
mvn flyway:info

# รีเซ็ตฐานข้อมูล (development เท่านั้น)
mvn flyway:clean
```

## ความต้องการหลัก (Core Requirements)

### 1. ระบบจัดการผู้ใช้ (User Management System)

- การลงทะเบียนและเข้าสู่ระบบ
- การจัดการโปรไฟล์ผู้ใช้
- ประวัติการสั่งซื้อ
- ดาวน์โหลดข้อมูลบัญชีที่ซื้อแล้วเป็นไฟล์ .txt

### 2. การจัดการแคตตาล็อกสินค้า (Product Catalog Management)

- จัดการหมวดหมู่สินค้าแบบลำดับชั้น
- เพิ่ม/แก้ไข/ลบสินค้า
- จัดการสต็อกและข้อมูลบัญชี
- การแจ้งเตือนเมื่อสต็อกต่ำ

### 3. การเรียกดูและค้นหาสินค้า (Product Browsing & Search)

- เรียกดูสินค้าตามหมวดหมู่
- ค้นหาและกรองสินค้า
- แสดงรายละเอียดสินค้า
- รองรับผู้ใช้หลายคนพร้อมกัน

### 4. การประมวลผลการชำระเงิน (Payment Processing)

- สร้าง QR Code สำหรับการชำระเงิน
- จองสินค้าระหว่างการชำระเงิน
- ติดตามสถานะการชำระเงิน
- ป้องกันการขายซ้ำ

### 5. แดชบอร์ดผู้ดูแลระบบ (Admin Dashboard)

- สถิติการขายและข้อมูลสต็อก
- จัดการผู้ใช้และคำสั่งซื้อ
- รายงานและการส่งออกข้อมูล
- การตรวจสอบระบบ

### 6. ความปลอดภัยของระบบ (System Security)

- การเข้ารหัสรหัสผ่านด้วย BCrypt
- JWT authentication พร้อม access และ refresh tokens
- Role-Based Access Control (RBAC)
- การตรวจสอบข้อมูลนำเข้าและ rate limiting
- CORS และ CSRF protection

### 7. การติดตามระบบ (System Observability)

- OpenTelemetry tracing สำหรับทุก request
- Structured logging ไปยัง Elasticsearch
- การจับข้อผิดพลาดและ troubleshooting
- การแจ้งเตือนเมื่อประสิทธิภาพเปลี่ยนแปลง

## โครงสร้างแพ็คเกจ (Package Structure)

- `config/` - การกำหนดค่าแอปพลิเคชัน รวมถึง security, database, observability
- `controller/` - REST API endpoints พร้อม error handling
- `service/` - Business logic layer (กำลังพัฒนา - ต้องการการ implement)
- `repository/` - Data access layer ใช้ Spring Data JPA
- `model/` - JPA entities ที่ extend BaseEntity พร้อม audit fields และ UUID primary keys
- `security/` - JWT authentication, rate limiting, และ security filters
- `exception/` - Custom exception hierarchy สำหรับ error handling
- `util/` - Utility classes รวมถึง database connection checker

## การออกแบบ Entity

Entity ทั้งหมด extend `BaseEntity` ซึ่งมี:

- UUID primary keys พร้อม `@GeneratedValue(strategy = GenerationType.UUID)`
- Audit fields: `createdAt`, `updatedAt` (ใช้ Hibernate timestamps)
- Optimistic locking ด้วย `@Version`
- การ implement equals/hashCode ที่เหมาะสม

## สถาปัตยกรรมความปลอดภัย (Security Architecture)

- JWT-based authentication พร้อม access และ refresh tokens
- Role-based authorization ใช้ Spring Security
- Rate limiting implement ด้วย Bucket4j
- Custom authentication filters และ entry points

## การกำหนดค่าฐานข้อมูล (Database Configuration)

- PostgreSQL ฐานข้อมูลหลักพร้อม HikariCP connection pooling
- H2 in-memory database สำหรับ tests
- Flyway migrations ใน `src/main/resources/db/migration/`
- Schema: `account_selling` พร้อม custom search path

## การตั้งค่า Observability

- OpenTelemetry auto-instrumentation ผ่าน Micrometer Tracing
- Structured logging พร้อม ECS format สำหรับ Elasticsearch
- Prometheus metrics endpoint: `/actuator/prometheus`
- Trace correlation ใน logs ผ่าน MDC
- OTLP endpoint กำหนดค่าสำหรับ external observability backend

## API Endpoints หลัก

### Authentication API

- `POST /api/auth/register` - ลงทะเบียนผู้ใช้ใหม่
- `POST /api/auth/login` - เข้าสู่ระบบ
- `POST /api/auth/refresh` - refresh token
- `POST /api/auth/logout` - ออกจากระบบ

### Product API

- `GET /api/categories` - ดูหมวดหมู่ทั้งหมด
- `GET /api/products` - ดูสินค้าทั้งหมด (พร้อมการกรอง)
- `GET /api/products/{id}` - ดูรายละเอียดสินค้า
- `GET /api/products/search` - ค้นหาสินค้า

### Payment API

- `POST /api/payments/generate` - สร้าง QR Code สำหรับชำระเงิน
- `GET /api/payments/status/{id}` - ตรวจสอบสถานะการชำระเงิน
- `POST /api/payments/webhook` - รับข้อมูลจาก payment gateway

### User API

- `GET /api/users/profile` - ดูโปรไฟล์ผู้ใช้
- `GET /api/users/orders` - ดูประวัติการสั่งซื้อ
- `GET /api/users/download/{orderId}` - ดาวน์โหลดข้อมูลบัญชี

### Admin API

- `POST/PUT/DELETE /api/admin/categories` - จัดการหมวดหมู่
- `POST/PUT/DELETE /api/admin/products` - จัดการสินค้า
- `POST/PUT /api/admin/stock` - จัดการสต็อก
- `GET /api/admin/dashboard` - ข้อมูลแดชบอร์ด

## การกำหนดค่าการทดสอบ (Testing Configuration)

Tests ใช้การกำหนดค่าแยก:

- H2 in-memory database
- TestContainers สำหรับ integration tests
- ปิดการใช้งาน Flyway migrations
- แยก application-test.yml profile

## เอกสาร API

Swagger UI พร้อมใช้งานที่: `http://localhost:8080/swagger-ui.html`
API docs JSON: `http://localhost:8080/api-docs`

## แผนการพัฒนา (Implementation Plan)

### Phase 1: Project Setup & Core Models ✅

- ตั้งค่าโครงการ Spring Boot
- กำหนดค่าฐานข้อมูลและ security
- สร้าง domain models หลัก
- ตั้งค่า OpenTelemetry และ logging

### Phase 2: Repository & Service Layer (กำลังดำเนินการ)

- พัฒนา repository layer สำหรับการเข้าถึงข้อมูล
- สร้าง service layer สำหรับ business logic
- เขียน unit tests

### Phase 3: API Layer

- พัฒนา REST controllers
- ใช้งาน input validation
- เขียน integration tests

### Phase 4: Security & Observability

- ใช้งาน JWT authentication
- ตั้งค่า role-based access control
- กำหนดค่า monitoring และ alerting

### Phase 5: Testing & Deployment

- integration และ performance testing
- สร้าง Docker configuration
- เตรียมเอกสารการ deploy

## หมายเหตุการ Implementation ที่สำคัญ

### Service Layer ที่ยังไม่ได้พัฒนา

Package service ยังว่างอยู่ เมื่อ implement services:

- ปฏิบัติตาม package structure ที่มีอยู่
- ใช้ `@Slf4j` สำหรับ logging
- Implement transaction boundaries ที่เหมาะสมด้วย `@Transactional`
- เพิ่ม OpenTelemetry tracing annotations หากจำเป็น

### การกำหนดค่า JWT

JWT secret และ expiration times กำหนดค่าใน application.yml สำหรับ production:

- ใช้ JWT secrets ที่ปลอดภัยและสุ่มขึ้น
- เก็บ secrets ใน environment variables หรือ secure configuration

### Database Migrations

- ใช้ Flyway สำหรับการเปลี่ยนแปลง schema
- วาง migration files ใน `src/main/resources/db/migration/`
- ปฏิบัติตาม naming convention: `V{version}__{description}.sql`

### Logging และ Tracing

- ใช้ `@Slf4j` annotation จาก Lombok
- Trace IDs รวมอยู่ใน logs โดยอัตโนมัติ
- เพิ่ม custom MDC fields สำหรับ business context เมื่อจำเป็น
- อ้างอิง `config/README-observability.md` สำหรับการใช้งานโดยละเอียด

### การจัดการข้อผิดพลาด (Exception Handling)

- Custom exception hierarchy ใน `exception/` package
- Extends `BaseException` สำหรับ error handling ที่สม่ำเสมอ
- Specific exceptions สำหรับ domains ต่างๆ (auth, payment, stock, etc.)

## โมเดลข้อมูลหลัก (Core Data Models)

### User Entity

- ข้อมูลบัญชีผู้ใช้ (username, password, email)
- บทบาทและสิทธิ์การเข้าถึง
- ประวัติการสั่งซื้อ

### Product & Category Entities

- หมวดหมู่สินค้าแบบลำดับชั้น
- ข้อมูลสินค้า (ชื่อ, ราคา, รูปภาพ, เซิร์ฟเวอร์)
- สถานะการใช้งาน

### Stock Entity

- ข้อมูลบัญชีที่เข้ารหัส
- สถานะการขายและการจอง
- การจัดการสต็อก

### Order & Payment Entities

- ข้อมูลคำสั่งซื้อและรายการสินค้า
- ข้อมูลการชำระเงินและสถานะ
- QR Code และ transaction reference

## กลยุทธ์การทดสอบ (Testing Strategy)

### Testing Levels

1. **Unit Testing** - ทดสอบ components แยกส่วน
2. **Integration Testing** - ทดสอบการทำงานร่วมกันของ components
3. **System Testing** - ทดสอบ end-to-end workflows

### Test Categories

1. **Functional Tests** - ทดสอบพฤติกรรมตาม requirements
2. **Security Tests** - ทดสอบการรักษาความปลอดภัย
3. **Performance Tests** - ทดสอบประสิทธิภาพ
4. **Concurrency Tests** - ทดสอบการทำงานพร้อมกัน

## สรุป

Account Selling Platform เป็นระบบที่ออกแบบมาเพื่อความปลอดภัย ความสามารถในการขยายตัว และความสะดวกในการบำรุงรักษา โดยใช้ Spring Boot และเทคโนโลยีที่ทันสมัย ระบบนี้จะให้บริการตลาดกลางที่เชื่อถือได้สำหรับการซื้อขายบัญชีดิจิทัล พร้อมด้วยเครื่องมือจัดการที่ครอบคลุมสำหรับผู้ดูแลระบบ
