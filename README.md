# Appointment Scheduling System

A robust, production-style Java backend application built with **Spring Boot**, **Spring Data JPA**, and **PostgreSQL** that enables users to book, reschedule, cancel, and track appointments while preventing double-bookings through time-interval conflict detection.

---

## Overview

The **Appointment Scheduling System** manages time-based bookings for service providers and clients. The application enforces business logic at the service layer, maintains relational integrity across database entities, and exposes a clean RESTful API for client interaction.

---

## Problem Statement

Manual scheduling or naive scheduling systems often suffer from:
- **Double Booking**: Concurrent or overlapping reservations for the same time window.
- **Boundary Collisions**: Inability to properly distinguish between contiguous (adjacent) slots and overlapping intervals.
- **Inconsistent State Transitions**: Rescheduling or cancelling already finalized or cancelled appointments without validation.
- **Unclear Slot Visibility**: Difficulty determining real-time available vs. booked slots on a specific date.

This backend application solves these issues through mathematical interval-based conflict detection, state validation, and structured REST endpoints.

---

## Key Features

- **User Management**: Onboard and retrieve users with email validation and unique constraint enforcement.
- **Appointment Booking**: Book time slots with positive duration validation and conflict detection against existing active bookings.
- **Mathematical Conflict Detection**: Detects overlapping intervals using half-open time ranges `[start, end)`.
- **Boundary-Aware Scheduling**: Correctly distinguishes adjacent appointments (e.g., `10:00–10:30` and `10:30–11:00`) as valid non-overlapping slots.
- **Rescheduling & Cancellation**: Move existing appointments to new dates/times (excluding the appointment's own ID from conflict checks) and cancel active bookings while guarding against invalid state transitions.
- **Dynamic Availability API**: Compute 30-minute availability slots across standard business hours (`09:00`–`17:00`) for any given date.
- **Centralized Exception Handling**: Structured, standard HTTP error responses (`400 Bad Request`, `404 Not Found`, `409 Conflict`, `500 Internal Server Error`).
- **Comprehensive Testing Suite**: Unit, WebMvc slice, and full lifecycle integration tests with JUnit 5 and Mockito.

---

## Tech Stack

| Layer / Tool | Technology | Purpose |
| :--- | :--- | :--- |
| **Language** | Java 17 (LTS) | Core programming language |
| **Framework** | Spring Boot 3.3.4 | Backend framework & dependency injection |
| **Web / REST** | Spring MVC (`spring-boot-starter-web`) | RESTful endpoint routing & request handling |
| **ORM / Persistence** | Spring Data JPA / Hibernate | Object-Relational Mapping & database queries |
| **Database** | PostgreSQL | Relational storage & ACID transactions |
| **Testing DB** | H2 Database (In-Memory) | Isolated test suite execution |
| **Validation** | Jakarta Bean Validation | Request payload validation |
| **Logging** | SLF4J / Logback | Structured application logging |
| **Testing** | JUnit 5, Mockito, Spring Boot Test | Unit, slice, and integration testing |
| **Build Tool** | Apache Maven | Dependency management & project build |

---

## System Architecture & Workflow

```
Client (HTTP / REST)
        ↓
Controller Layer (Validation & HTTP Mapping)
        ↓
Service Layer (Business Rules & Conflict Detection)
        ↓
Repository Layer (Spring Data JPA / JPQL Queries)
        ↓
PostgreSQL Database
```

### Request Lifecycle
1. **Request Intake**: Client issues an HTTP request (`POST`, `GET`, `PUT`, `DELETE`) with JSON payload.
2. **DTO Validation**: Controller validates request syntax using Jakarta Bean Validation (`@Valid`).
3. **Business & Conflict Check**: Service verifies temporal logic (non-past dates, `startTime < endTime`) and queries the repository for active overlapping intervals.
4. **Persistence**: JPA entities are committed inside an active transaction.
5. **Response / Error Mapping**: Response DTO or standardized `ErrorResponseDto` is returned with the appropriate HTTP status code.

---

## Project Structure

```
appointment-scheduling-system/
├── .mvn/wrapper/                  # Maven wrapper binaries and configuration
├── src/
│   ├── main/
│   │   ├── java/com/appointmentscheduling/
│   │   │   ├── AppointmentSchedulingApplication.java  # Application Entry Point
│   │   │   ├── controller/                             # REST API Controllers
│   │   │   │   ├── AppointmentController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto/                                    # Request & Response Data Transfer Objects
│   │   │   │   ├── AppointmentRequestDto.java
│   │   │   │   ├── AppointmentResponseDto.java
│   │   │   │   ├── AvailabilitySlotDto.java
│   │   │   │   ├── ErrorResponseDto.java
│   │   │   │   ├── RescheduleRequestDto.java
│   │   │   │   ├── UserRequestDto.java
│   │   │   │   └── UserResponseDto.java
│   │   │   ├── entity/                                 # JPA Database Entities
│   │   │   │   ├── Appointment.java
│   │   │   │   └── User.java
│   │   │   ├── enums/                                  # Domain Enumerations
│   │   │   │   └── AppointmentStatus.java
│   │   │   ├── exception/                              # Custom Exceptions & Global Handler
│   │   │   │   ├── AppointmentNotFoundException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── InvalidAppointmentException.java
│   │   │   │   ├── ScheduleConflictException.java
│   │   │   │   ├── UserAlreadyExistsException.java
│   │   │   │   └── UserNotFoundException.java
│   │   │   ├── repository/                             # Spring Data JPA Repositories
│   │   │   │   ├── AppointmentRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   └── service/                                # Business Logic Services
│   │   │       ├── AppointmentService.java
│   │   │       └── UserService.java
│   │   └── resources/
│   │       ├── application.properties                  # PostgreSQL Configuration
│   │       └── schema.sql                              # Database DDL Schema Reference
│   └── test/
│       ├── java/com/appointmentscheduling/
│       │   ├── controller/                             # Controller Slice Tests (WebMvcTest)
│       │   │   ├── AppointmentControllerTest.java
│       │   │   └── UserControllerTest.java
│       │   ├── integration/                            # End-to-End Integration Tests
│       │   │   └── AppointmentSchedulingIntegrationTest.java
│       │   └── service/                                # Business Logic Unit Tests (Mockito)
│       │       ├── AppointmentServiceTest.java
│       │       └── UserServiceTest.java
│       └── resources/
│           └── application.properties                  # H2 Test Profile Configuration
├── CODE_REVIEW_CHECKLIST.md                            # Code Quality Checklist & Self-Audit
├── DESIGN_DECISIONS.md                                 # Architecture Rationale & Trade-offs
├── DEVELOPMENT.md                                      # Agile Iterations & Debugging Records
├── INTERVIEW.md                                        # Technical Interview Preparation Guide
├── pom.xml                                             # Maven Dependencies & Build Setup
└── README.md                                           # Project Documentation
```

---

## How It Works

### 1. Conflict Detection Algorithm

Appointments are modeled as half-open time intervals:
$$[T_{\text{start}}, T_{\text{end}})$$

An overlap occurs with an existing active appointment (`BOOKED`, `CONFIRMED`) on the same date if and only if:
$$\text{requestedStart} < \text{existingEnd} \quad \text{AND} \quad \text{requestedEnd} > \text{existingStart}$$

```
Case A (Overlapping -> 409 Conflict):
Existing: [ 10:00 --------------- 10:30 )
New:             [ 10:15 --------------- 10:45 )
Condition: 10:15 < 10:30 (TRUE) AND 10:45 > 10:00 (TRUE) -> CONFLICT

Case B (Adjacent Boundary -> 201 Created):
Existing: [ 10:00 --------- 10:30 )
New:                        [ 10:30 --------- 11:00 )
Condition: 10:30 < 10:30 (FALSE) -> ALLOWED
```

### 2. Rescheduling Logic

When rescheduling an appointment:
1. Verify the appointment exists.
2. Confirm the appointment status is neither `CANCELLED` nor `COMPLETED`.
3. Validate that the new start time is strictly before the new end time and that the date is not in the past.
4. Execute conflict detection while **excluding the current appointment's ID** to prevent false self-collisions.
5. Update date, time, and `updatedAt` timestamp.

### 3. Slot Availability Generation

The availability service calculates available vs. booked slots for any requested date:
- Operating Window: `09:00` to `17:00`
- Slot Step: `30 minutes`
- Evaluates active bookings on that date. If any booking overlaps with a 30-minute block, the block is labeled `BOOKED`; otherwise, `AVAILABLE`.

---

## API Endpoints

### User Endpoints

| Method | Endpoint | Purpose | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/users` | Register a new user | `201 Created` |
| `GET` | `/api/users/{id}` | Fetch user details by ID | `200 OK` / `404 Not Found` |
| `GET` | `/api/users` | Retrieve all registered users | `200 OK` |

### Appointment Endpoints

| Method | Endpoint | Purpose | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/appointments` | Book a new appointment | `201 Created` / `409 Conflict` |
| `GET` | `/api/appointments/{id}` | Get appointment details by ID | `200 OK` / `404 Not Found` |
| `GET` | `/api/appointments` | List appointments (filter by `userId`, `date`, `status`) | `200 OK` |
| `PUT` | `/api/appointments/{id}` | Reschedule an appointment | `200 OK` / `400` / `409` |
| `PUT` | `/api/appointments/{id}/cancel` | Cancel an active appointment | `200 OK` / `400 Bad Request` |
| `DELETE` | `/api/appointments/{id}` | Delete an appointment record | `204 No Content` / `404` |
| `GET` | `/api/appointments/availability` | Check 30-min slot availability for a date | `200 OK` |

---

## Database Schema

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'BOOKED',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_appointment_date ON appointments(appointment_date);
CREATE INDEX idx_appointment_user ON appointments(user_id);
CREATE INDEX idx_appointment_status ON appointments(status);
```

---

## Installation & Local Setup

### Prerequisites
- **Java 17** or higher
- **Maven 3.8+** (or use the included `./mvnw` wrapper)
- **PostgreSQL 14+** (for standard execution)

### 1. Clone the Repository
```bash
git clone https://github.com/harsharaju1314-hash/Appointment-Scheduling-System.git
cd Appointment-Scheduling-System
```

### 2. Configure PostgreSQL Database
Create a local database named `appointment_db`:
```sql
CREATE DATABASE appointment_db;
```

Update credentials in `src/main/resources/application.properties` or set environment variables:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/appointment_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 3. Build the Application
```bash
# Windows
mvnw.cmd clean package

# Linux / macOS
./mvnw clean package
```

### 4. Run the Application
```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```
The server will start at `http://localhost:8080`.

---

## Running Tests

The test suite runs automatically with an embedded H2 database without requiring PostgreSQL to be running.

```bash
# Execute all unit, controller slice, and integration tests
mvnw.cmd test
```

### Test Coverage Highlights
- **14 Unit Tests in `AppointmentServiceTest`**: Validates creation, start >= end checking, conflict detection, adjacent boundary allowance, reschedule conflict checking, self-exclusion during reschedule, cancellation, and slot availability calculation.
- **5 Unit Tests in `UserServiceTest`**: Validates creation, duplicate email rejection, single retrieval, and list retrieval.
- **14 Controller Tests in `AppointmentControllerTest` & `UserControllerTest`**: Verifies REST endpoints, HTTP status codes, and Bean Validation constraints.
- **End-to-End Integration Test in `AppointmentSchedulingIntegrationTest`**: Exercises full user registration, booking, conflict collision, boundary booking, rescheduling, and cancellation flow.

---

## Sample Usage (cURL)

### 1. Create a User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Evelyn Reed",
    "email": "evelyn.reed@example.com"
  }'
```
**Response (`201 Created`):**
```json
{
  "id": 1,
  "name": "Evelyn Reed",
  "email": "evelyn.reed@example.com",
  "createdAt": "2026-10-01T10:00:00"
}
```

### 2. Book an Appointment
```bash
curl -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "appointmentDate": "2026-10-05",
    "startTime": "10:00",
    "endTime": "10:30"
  }'
```
**Response (`201 Created`):**
```json
{
  "id": 1,
  "userId": 1,
  "userName": "Evelyn Reed",
  "userEmail": "evelyn.reed@example.com",
  "appointmentDate": "2026-10-05",
  "startTime": "10:00",
  "endTime": "10:30",
  "status": "BOOKED",
  "createdAt": "2026-10-01T10:05:00",
  "updatedAt": "2026-10-01T10:05:00"
}
```

### 3. Attempt Conflicting Booking (10:15 - 10:45)
```bash
curl -X POST http://localhost:8080/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "appointmentDate": "2026-10-05",
    "startTime": "10:15",
    "endTime": "10:45"
  }'
```
**Response (`409 Conflict`):**
```json
{
  "timestamp": "2026-10-01T10:06:00",
  "status": 409,
  "error": "Conflict",
  "message": "Time slot [10:15 - 10:45] conflicts with existing appointment from 10:00 to 10:30",
  "path": "/api/appointments"
}
```

### 4. Check Date Availability
```bash
curl -X GET "http://localhost:8080/api/appointments/availability?date=2026-10-05"
```
**Response (`200 OK`):**
```json
[
  { "startTime": "09:00", "endTime": "09:30", "status": "AVAILABLE" },
  { "startTime": "09:30", "endTime": "10:00", "status": "AVAILABLE" },
  { "startTime": "10:00", "endTime": "10:30", "status": "BOOKED" },
  { "startTime": "10:30", "endTime": "11:00", "status": "AVAILABLE" }
]
```

### 5. Reschedule an Appointment
```bash
curl -X PUT http://localhost:8080/api/appointments/1 \
  -H "Content-Type: application/json" \
  -d '{
    "appointmentDate": "2026-10-05",
    "startTime": "14:00",
    "endTime": "14:30"
  }'
```

### 6. Cancel an Appointment
```bash
curl -X PUT http://localhost:8080/api/appointments/1/cancel
```

---

## Future Improvements

1. **Authentication & Authorization**: Integrate Spring Security with JWT tokens for role-based access (User vs. Service Provider/Admin).
2. **Dynamic Operating Schedules**: Support customizable business hours, break periods, and custom slot durations per service provider.
3. **Notification Service**: Event-driven email or SMS confirmations on booking and cancellation.

---

## What I Learned

- Designing strict temporal boundary rules using half-open intervals `[start, end)` in relational database queries.
- Managing database state transitions and avoiding self-collision bugs during entity updates.
- Writing isolated, fast unit tests using Mockito and Spring Boot `@WebMvcTest` test slices.
- Implementing centralized exception handling with `@RestControllerAdvice` to maintain standardized API error contracts.

---

## Author

- **Harshavardhan Varma** — [GitHub Profile](https://github.com/harsharaju1314-hash)
- **Repository**: [https://github.com/harsharaju1314-hash/Appointment-Scheduling-System](https://github.com/harsharaju1314-hash/Appointment-Scheduling-System)
