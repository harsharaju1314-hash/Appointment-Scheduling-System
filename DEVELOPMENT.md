# Development Log & Agile Iterations

This document details the development lifecycle, iterative milestones, and technical debugging issues resolved during the implementation of the **Appointment Scheduling System**.

---

## 1. Agile Development Iterations

The project followed an iterative four-phase development lifecycle:

### Iteration 1 — Core Domain & CRUD APIs
- **Goal**: Establish project architecture, database models, and baseline CRUD endpoints.
- **Completed**:
  - Initialized Spring Boot 3.3.4 project with Java 17 and Maven.
  - Designed relational database schema in PostgreSQL for `User` and `Appointment` entities.
  - Implemented `UserRepository` and `AppointmentRepository` with Spring Data JPA.
  - Implemented `UserService` and `UserController` for user onboarding (`POST /api/users`, `GET /api/users/{id}`, `GET /api/users`).
  - Added Jakarta validation (`@NotBlank`, `@Email`, `@Size`, `@NotNull`) to DTOs.

### Iteration 2 — Scheduling Logic, Rescheduling & Availability
- **Goal**: Implement the core domain logic for conflict detection, rescheduling, cancellation, and availability checking.
- **Completed**:
  - Defined appointment status lifecycle (`BOOKED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`).
  - Implemented interval-based conflict detection (`startTime < existingEnd && endTime > existingStart`).
  - Implemented boundary-aware scheduling rules (half-open intervals `[start, end)` allowing adjacent bookings).
  - Built `rescheduleAppointment` endpoint with state-transition checks and self-exclusion conflict validation.
  - Built `cancelAppointment` endpoint preventing cancellation of completed or already cancelled records.
  - Built `getAvailability` endpoint generating 30-minute availability slots across standard business hours (09:00 - 17:00).

### Iteration 3 — Reliability, Exception Handling & Unit Testing
- **Goal**: Harden application reliability through centralized error handling, logging, and comprehensive test coverage.
- **Completed**:
  - Built `@RestControllerAdvice` (`GlobalExceptionHandler`) handling 400 Bad Request, 404 Not Found, 409 Conflict, and 500 Internal Server Error with structured `ErrorResponseDto`.
  - Added structured SLF4J logging across controller and service boundaries.
  - Implemented unit test suites using JUnit 5 and Mockito for `UserServiceTest` and `AppointmentServiceTest`.
  - Added `@WebMvcTest` controller slice tests (`UserControllerTest`, `AppointmentControllerTest`) verifying HTTP status codes and JSON payload validation.

### Iteration 4 — Integration Testing, Refinement & Code Quality Review
- **Goal**: End-to-end integration validation, documentation, and code review audit.
- **Completed**:
  - Built full lifecycle integration test `AppointmentSchedulingIntegrationTest` using `@SpringBootTest` and MockMvc.
  - Verified edge cases: past-date bookings, start >= end validation, duplicate email handling, boundary bookings.
  - Conducted code quality review using `CODE_REVIEW_CHECKLIST.md`.
  - Documented architectural decisions in `DESIGN_DECISIONS.md` and interview guide in `INTERVIEW.md`.

---

## 2. Development Issues & Debugging Records

Below are real technical challenges encountered during the implementation of the scheduling engine and how they were resolved.

---

### Issue 1: Adjacent Time Slot False Conflict in Overlap Query

- **Problem**: When a user attempted to book an appointment immediately after an existing appointment (e.g., existing: `10:00 - 10:30`, new request: `10:30 - 11:00`), the system erroneously returned `409 Conflict`.
- **Investigation**: Inspected the JPQL query executed in `AppointmentRepository`. The original query was using non-strict comparison operators:
  ```sql
  WHERE appointment_date = :date AND start_time <= :endTime AND end_time >= :startTime
  ```
  When evaluating `10:30 <= 10:30` and `11:00 >= 10:00`, both evaluated to `true`, flagging contiguous appointments as overlapping.
- **Root Cause**: The scheduling model treats time slots as half-open intervals `[start, end)`. Touching at the boundary (`existingEnd == newStart`) is a valid contiguous booking, not an overlap.
- **Fix**: Adjusted the comparison logic in `AppointmentRepository` to use strict inequalities:
  ```java
  @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date " +
         "AND a.status IN (:statuses) " +
         "AND a.startTime < :endTime " +
         "AND a.endTime > :startTime")
  List<Appointment> findOverlappingAppointments(
          @Param("date") LocalDate date,
          @Param("startTime") LocalTime startTime,
          @Param("endTime") LocalTime endTime,
          @Param("statuses") Collection<AppointmentStatus> statuses
  );
  ```
- **Test Added to Prevent Regression**: Added `shouldAllowAdjacentAppointmentAtBoundary()` in `AppointmentServiceTest` and verified contiguous bookings `10:00-10:30` followed by `10:30-11:00` return HTTP 201.

---

### Issue 2: Self-Conflict on Appointment Rescheduling

- **Problem**: When attempting to reschedule an appointment without modifying its time (or moving it by a small interval on the same day), the service flagged the appointment as conflicting with itself, returning `409 Conflict`.
- **Investigation**: Traced execution in `AppointmentService.rescheduleAppointment()`. The method called `findOverlappingAppointments()`, which searched all active appointments on that date without excluding the record being updated (`appointment.getId()`).
- **Root Cause**: The repository query did not filter out the existing appointment record ID, leading to a collision with its own database row.
- **Fix**: Created a dedicated repository query `findOverlappingAppointmentsExcludingId`:
  ```java
  @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date " +
         "AND a.id <> :excludeId " +
         "AND a.status IN (:statuses) " +
         "AND a.startTime < :endTime " +
         "AND a.endTime > :startTime")
  List<Appointment> findOverlappingAppointmentsExcludingId(
          @Param("date") LocalDate date,
          @Param("startTime") LocalTime startTime,
          @Param("endTime") LocalTime endTime,
          @Param("statuses") Collection<AppointmentStatus> statuses,
          @Param("excludeId") Long excludeId
  );
  ```
- **Test Added to Prevent Regression**: Added unit tests verifying `findOverlappingAppointmentsExcludingId` excludes the target ID during rescheduling, and verified in `AppointmentSchedulingIntegrationTest`.
