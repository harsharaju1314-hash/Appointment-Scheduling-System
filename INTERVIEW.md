# Interview Preparation Guide: Appointment Scheduling System

This guide provides concise, technical answers for explaining the **Appointment Scheduling System** project in software engineering interviews.

---

### 1. Explain the project.
**Answer:**
"I built an **Appointment Scheduling System** backend in Java and Spring Boot that enables users to book, reschedule, cancel, and track appointments while strictly preventing overlapping bookings. The core feature is a robust time-interval conflict detection algorithm that treats appointment slots as half-open intervals `[start, end)`. The system also provides a dynamic availability API to view open and booked 30-minute slots for any date."

---

### 2. Why Java?
**Answer:**
"I chose Java (version 17 LTS) for its strong static type system, memory management, and modern date/time APIs (`LocalDate`, `LocalTime`). The compile-time type safety ensures that temporal comparisons and domain models are strictly validated before runtime, minimizing errors."

---

### 3. Why Spring Boot?
**Answer:**
"Spring Boot provides an opinionated framework that eliminates boilerplate configuration through starters like `spring-boot-starter-web` and `spring-boot-starter-data-jpa`. Its built-in Dependency Injection container allows clean decoupling between controllers, services, and repositories, which makes writing isolated unit tests with Mockito straightforward."

---

### 4. Why PostgreSQL?
**Answer:**
"PostgreSQL provides robust ACID transactional compliance and strong relational integrity. Because appointment scheduling depends on strict relational constraints (linking users to appointments) and indexed temporal queries across date and time columns, PostgreSQL ensures high consistency and avoids corrupted booking states."

---

### 5. Why JPA / Hibernate?
**Answer:**
"Spring Data JPA with Hibernate simplifies Object-Relational Mapping (ORM), reducing repetitive boilerplate SQL. It allowed me to write expressive JPQL queries for overlap detection while handling automatic entity state transitions and database schema synchronizations."

---

### 6. Explain your architecture.
**Answer:**
"The project follows a standard layered 3-tier architecture:
- **Controller Layer (`com.appointmentscheduling.controller`)**: Accepts HTTP requests, validates incoming DTO payloads with Jakarta Validation (`@Valid`), and maps responses to appropriate HTTP status codes.
- **Service Layer (`com.appointmentscheduling.service`)**: Encapsulates business logic, temporal validations, slot availability calculations, and conflict detection rules.
- **Repository Layer (`com.appointmentscheduling.repository`)**: Interacts with the PostgreSQL database using Spring Data JPA.
- **DTOs & Entities**: Request and Response DTOs isolate database schemas from external API contracts."

---

### 7. How does conflict detection work?
**Answer:**
"Conflict detection models appointment time slots as half-open intervals `[start, end)`. An overlap occurs between an existing active appointment and a new booking request on the same date if and only if:
$$\text{newStartTime} < \text{existingEndTime} \quad \text{AND} \quad \text{newEndTime} > \text{existingStartTime}$$
Only active appointments (`BOOKED`, `CONFIRMED`) trigger conflicts. Cancelled appointments are excluded."

---

### 8. How do you handle boundary conditions (e.g. 10:00–10:30 and 10:30–11:00)?
**Answer:**
"Because slots are treated as half-open intervals `[start, end)`, touching at exact boundaries is non-overlapping.
For example, if an appointment exists from `10:00 to 10:30` and a new request is `10:30 to 11:00`:
- `10:30 < 10:30` evaluates to `false`.
Therefore, no conflict is raised and the booking is allowed. Using strict inequality operators (`<` and `>`) in the JPQL query enforces this rule."

---

### 9. What happens when two appointments overlap?
**Answer:**
"When an overlap is detected during appointment creation or rescheduling, the service layer throws a custom `ScheduleConflictException`. The centralized `@RestControllerAdvice` (`GlobalExceptionHandler`) intercepts this exception and translates it into an HTTP `409 Conflict` response with a structured JSON error body detailing the conflicting time interval."

---

### 10. How do you handle exceptions centrally?
**Answer:**
"I implemented `GlobalExceptionHandler` annotated with `@RestControllerAdvice`. It uses `@ExceptionHandler` methods to intercept domain exceptions:
- `UserNotFoundException` & `AppointmentNotFoundException` $\rightarrow$ `404 Not Found`
- `ScheduleConflictException` & `UserAlreadyExistsException` $\rightarrow$ `409 Conflict`
- `InvalidAppointmentException` & `MethodArgumentNotValidException` $\rightarrow$ `400 Bad Request`
- Unhandled `Exception` $\rightarrow$ `500 Internal Server Error`
All errors return a consistent `ErrorResponseDto` containing timestamps, status codes, error types, error messages, and validation field details."

---

### 11. How did you debug an issue during development?
**Answer:**
"During rescheduling implementation, I encountered a bug where rescheduling an appointment without changing its start/end time caused the system to return `409 Conflict`.
- **Investigation**: I inspected service logs and found that the conflict query checked all active appointments on that date, including the record currently being modified.
- **Fix**: I created a custom repository query `findOverlappingAppointmentsExcludingId` that takes an `excludeId` parameter (`AND a.id <> :excludeId`).
- **Verification**: I added unit tests and integration tests to verify that rescheduling does not collide with the appointment's own database row."

---

### 12. How did you test the project?
**Answer:**
"I wrote a comprehensive test suite using **JUnit 5**, **Mockito**, and **Spring Boot Test**:
1. **Unit Tests (`AppointmentServiceTest`, `UserServiceTest`)**: Mocked repository dependencies to test business edge cases such as invalid time spans, start >= end, past dates, duplicate emails, boundary bookings, and invalid state transitions.
2. **Controller Slice Tests (`AppointmentControllerTest`, `UserControllerTest`)**: Used `@WebMvcTest` and `MockMvc` to verify HTTP status codes, request serialization, and Bean Validation error formatting.
3. **Integration Tests (`AppointmentSchedulingIntegrationTest`)**: Used `@SpringBootTest` with an in-memory H2 database in PostgreSQL mode to test the end-to-end lifecycle from user creation to booking, conflict prevention, rescheduling, and cancellation."

---

### 13. What is Dependency Injection (DI)?
**Answer:**
"Dependency Injection is a design pattern implementing Inversion of Control (IoC), where an object's dependencies are supplied by an external container rather than instantiated directly by the class using `new`. In my project, Spring injects repository dependencies into services and service dependencies into controllers via constructor injection, enabling loose coupling and easy mocking during tests."

---

### 14. What is REST?
**Answer:**
"REST (Representational State Transfer) is an architectural style for networked applications using standard HTTP methods:
- `POST` to create resources (`/api/appointments` $\rightarrow$ `201 Created`)
- `GET` to read resources (`/api/appointments/{id}` $\rightarrow$ `200 OK`)
- `PUT` to update or reschedule resources (`/api/appointments/{id}` $\rightarrow$ `200 OK`)
- `DELETE` to remove resources (`/api/appointments/{id}` $\rightarrow$ `204 No Content`)
It uses stateless communication and standard status codes (`200`, `201`, `400`, `404`, `409`)."

---

### 15. What is JPA?
**Answer:**
"JPA (Jakarta Persistence API) is a specification in Java that defines how Java objects are mapped to relational database tables. Hibernate is the underlying ORM provider implementing JPA in Spring Data JPA."

---

### 16. How did you use Git?
**Answer:**
"I structured development using Git feature branches:
- `feature/user-api`
- `feature/appointment-api`
- `feature/conflict-detection`
- `feature/availability`
- `feature/testing`
Each branch contained atomic, meaningful commits that were merged into `main` after running the automated test suite."

---

### 17. How did you follow Agile principles?
**Answer:**
"I organized development into four iterative milestones:
1. Iteration 1: Core Domain Entities and Basic CRUD APIs
2. Iteration 2: Scheduling, Conflict Detection, and Availability Logic
3. Iteration 3: Centralized Exception Handling, Logging, and Unit Testing
4. Iteration 4: End-to-End Integration Testing, Code Review Audit, and Documentation"

---

### 18. How would you handle a code review?
**Answer:**
"I use a structured review checklist (`CODE_REVIEW_CHECKLIST.md`) focusing on:
- Boundary conditions and mathematical edge cases
- Input validation and exception handling
- DTO decoupling and domain encapsulation
- Test coverage for positive and negative paths
- Clean naming conventions and absence of code duplication
When receiving feedback, I verify the rationale, write regression tests for any reported issues, and refactor cleanly."

---

### 19. What would you improve in future iterations?
**Answer:**
"Future improvements would include:
1. Adding JWT-based Spring Security for authentication and role-based access control (e.g., user vs admin/doctor).
2. Implementing configurable business hours and custom slot durations per user/service provider.
3. Adding email/SMS notification integration on booking confirmation and cancellation."
