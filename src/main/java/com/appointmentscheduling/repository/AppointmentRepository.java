package com.appointmentscheduling.repository;

import com.appointmentscheduling.entity.Appointment;
import com.appointmentscheduling.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByAppointmentDate(LocalDate appointmentDate);

    List<Appointment> findByUserId(Long userId);

    List<Appointment> findByStatus(AppointmentStatus status);

    List<Appointment> findByAppointmentDateAndStatusIn(LocalDate date, Collection<AppointmentStatus> statuses);

    /**
     * Finds overlapping appointments for a given date and time range.
     * Boundary rule: [start, end) half-open intervals.
     * Overlap condition: (newStart < existingEnd) AND (newEnd > existingStart).
     * Only checks active statuses (e.g. BOOKED, CONFIRMED).
     */
    @Query("SELECT a FROM Appointment a " +
           "WHERE a.appointmentDate = :date " +
           "AND a.status IN (:statuses) " +
           "AND a.startTime < :endTime " +
           "AND a.endTime > :startTime")
    List<Appointment> findOverlappingAppointments(
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") Collection<AppointmentStatus> statuses
    );

    /**
     * Finds overlapping appointments excluding a specific appointment ID (used during rescheduling).
     */
    @Query("SELECT a FROM Appointment a " +
           "WHERE a.appointmentDate = :date " +
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
}
