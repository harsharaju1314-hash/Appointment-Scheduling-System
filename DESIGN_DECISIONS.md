# Design Decisions & Architecture Trade-offs

This document outlines the architectural rationale, technology choices, and design trade-offs made during the development of the **Appointment Scheduling System**.

---

## 1. Technology Choices & Rationale

### 1.1 Java 17 LTS
- **Decision**: Use Java 17 as the baseline language version.
- **Rationale**:
  - Java 17 provides Long-Term Support (LTS), delivering modern features such as enhanced pattern matching, modern switch expressions, improved JVM garbage collectors, and records.
  - Strong compile-time type safety ensures that date/time types (`LocalDate`, `LocalTime`, `LocalDateTime`) are strictly enforced, avoiding runtime temporal errors common in dynamically typed languages.

### 1.2 Spring Boot 3.x
- **Decision**: Build the application using Spring Boot.
- **Rationale**:
  - Eliminates boilerplate configuration through opinionated starters (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`).
  - Embedded web server (Tomcat) simplifies local deployment and execution.
  - Built-in Dependency Injection (DI) and Inversion of Control (IoC) allow decoupled service components, making unit testing straightforward with Mockito.

### 1.3 PostgreSQL
- **Decision**: Use PostgreSQL as the relational database engine.
- **Rationale**:
  - Appointment scheduling requires strict ACID guarantees. Overlapping booking prevention relies on atomic transactions and relational consistency.
  - Foreign key constraints (e.g., `user_id` referencing `users(id)`) and index support on `appointment_date`, `user_id`, and `status` optimize query performance during time interval overlap checks.

### 1.4 Spring Data JPA & Hibernate
- **Decision**: Use Spring Data JPA over raw JDBC / MyBatis.
- **Rationale**:
  - Clean Object-Relational Mapping (ORM) between domain entities (`User`, `Appointment`) and relational tables.
  - Dynamic query generation (`findByUserId`, `findByAppointmentDate`) reduces SQL boilerplate.
  - JPQL allows concise interval comparison queries (`startTime < :endTime AND endTime > :startTime`) while remaining database agnostic.

### 1.5 JUnit 5 & Mockito
- **Decision**: Use JUnit 5 and Mockito for automated testing.
- **Rationale**:
  - JUnit 5 provides clean assertions, nested test structures, and descriptive `@DisplayName` annotations.
  - Mockito allows mocking repository calls in the business service layer, ensuring fast, isolated unit tests without requiring a live database connection for every test case.

### 1.6 Layered Architecture (Controller → Service → Repository)
- **Decision**: Standard 3-tier layered architecture.
- **Rationale**:
  - **Controller Layer**: Handles HTTP protocol concerns, input validation (`@Valid`), and mapping HTTP status codes.
  - **Service Layer**: Encapsulates core scheduling algorithms, interval overlap checks, and status transition rules.
  - **Repository Layer**: Manages database persistence, queries, and data extraction.
  - Strict separation ensures business logic remains independent of transport protocols and UI concerns.

---

## 2. Alternatives Considered & Evaluated

### Alternative 1: Spring WebFlux (Reactive Stack) vs. Spring MVC (Servlet Stack)
- **Evaluated**: Using Project Reactor / Spring WebFlux with R2DBC for non-blocking reactive streams.
- **Decision**: Rejected in favor of standard Spring MVC (Servlet stack).
- **Reasoning**:
  - For a small-to-medium appointment scheduling system, synchronous request-response flow is clean, predictable, and easier to debug.
  - Reactive programming introduces debugging complexity, stack trace obfuscation, and cognitive overhead that is unnecessary for typical scheduling workloads where database I/O is bounded.

### Alternative 2: NoSQL / Document Store (e.g., MongoDB) vs. Relational (PostgreSQL)
- **Evaluated**: Storing appointments as JSON documents in MongoDB.
- **Decision**: Rejected in favor of PostgreSQL.
- **Reasoning**:
  - Appointment booking fundamentally relies on relational integrity (appointments belong to users) and multi-field indexed queries (`date`, `startTime`, `endTime`, `status`).
  - Relational databases provide straightforward row-level transactional isolation and foreign key enforcement to guarantee data integrity across concurrent updates.
