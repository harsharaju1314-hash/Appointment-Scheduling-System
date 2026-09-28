package com.appointmentscheduling.service;

import com.appointmentscheduling.dto.AppointmentRequestDto;
import com.appointmentscheduling.dto.AppointmentResponseDto;
import com.appointmentscheduling.dto.AvailabilitySlotDto;
import com.appointmentscheduling.dto.RescheduleRequestDto;
import com.appointmentscheduling.entity.Appointment;
import com.appointmentscheduling.entity.User;
import com.appointmentscheduling.enums.AppointmentStatus;
import com.appointmentscheduling.exception.AppointmentNotFoundException;
import com.appointmentscheduling.exception.InvalidAppointmentException;
import com.appointmentscheduling.exception.ScheduleConflictException;
import com.appointmentscheduling.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    private static final List<AppointmentStatus> ACTIVE_STATUSES = Arrays.asList(
            AppointmentStatus.BOOKED,
            AppointmentStatus.CONFIRMED
    );

    private static final LocalTime DAY_START = LocalTime.of(9, 0);
    private static final LocalTime DAY_END = LocalTime.of(17, 0);
    private static final int SLOT_DURATION_MINUTES = 30;

    private final AppointmentRepository appointmentRepository;
    private final UserService userService;

    public AppointmentService(AppointmentRepository appointmentRepository, UserService userService) {
        this.appointmentRepository = appointmentRepository;
        this.userService = userService;
    }

    public AppointmentResponseDto createAppointment(AppointmentRequestDto request) {
        logger.info("Received request to create appointment for user ID: {} on {} from {} to {}",
                request.getUserId(), request.getAppointmentDate(), request.getStartTime(), request.getEndTime());

        validateAppointmentTime(request.getAppointmentDate(), request.getStartTime(), request.getEndTime());

        User user = userService.getUserEntityById(request.getUserId());

        checkTimeConflicts(request.getAppointmentDate(), request.getStartTime(), request.getEndTime(), null);

        Appointment appointment = new Appointment();
        appointment.setUser(user);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(request.getEndTime());
        appointment.setStatus(AppointmentStatus.BOOKED);

        Appointment saved = appointmentRepository.save(appointment);
        logger.info("Successfully created appointment ID: {} for user ID: {}", saved.getId(), user.getId());

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public AppointmentResponseDto getAppointmentById(Long id) {
        logger.debug("Fetching appointment by ID: {}", id);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        return mapToDto(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> getAllAppointments(Long userId, LocalDate date, AppointmentStatus status) {
        logger.debug("Fetching appointments with filters - userId: {}, date: {}, status: {}", userId, date, status);

        List<Appointment> list;
        if (userId != null) {
            list = appointmentRepository.findByUserId(userId);
        } else if (date != null) {
            list = appointmentRepository.findByAppointmentDate(date);
        } else if (status != null) {
            list = appointmentRepository.findByStatus(status);
        } else {
            list = appointmentRepository.findAll();
        }

        return list.stream()
                .filter(a -> date == null || a.getAppointmentDate().equals(date))
                .filter(a -> status == null || a.getStatus() == status)
                .filter(a -> userId == null || (a.getUser() != null && a.getUser().getId().equals(userId)))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public AppointmentResponseDto rescheduleAppointment(Long id, RescheduleRequestDto request) {
        logger.info("Received request to reschedule appointment ID: {} to {} from {} to {}",
                id, request.getAppointmentDate(), request.getStartTime(), request.getEndTime());

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            logger.warn("Reschedule rejected: Appointment ID {} is CANCELLED", id);
            throw new InvalidAppointmentException("Cannot reschedule a cancelled appointment");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            logger.warn("Reschedule rejected: Appointment ID {} is COMPLETED", id);
            throw new InvalidAppointmentException("Cannot reschedule a completed appointment");
        }

        validateAppointmentTime(request.getAppointmentDate(), request.getStartTime(), request.getEndTime());

        checkTimeConflicts(request.getAppointmentDate(), request.getStartTime(), request.getEndTime(), id);

        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setStartTime(request.getStartTime());
        appointment.setEndTime(request.getEndTime());

        Appointment updated = appointmentRepository.save(appointment);
        logger.info("Successfully rescheduled appointment ID: {}", updated.getId());

        return mapToDto(updated);
    }

    public AppointmentResponseDto cancelAppointment(Long id) {
        logger.info("Received request to cancel appointment ID: {}", id);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            logger.warn("Cancellation rejected: Appointment ID {} is already CANCELLED", id);
            throw new InvalidAppointmentException("Appointment is already cancelled");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            logger.warn("Cancellation rejected: Appointment ID {} is already COMPLETED", id);
            throw new InvalidAppointmentException("Cannot cancel a completed appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);
        logger.info("Successfully cancelled appointment ID: {}", id);

        return mapToDto(saved);
    }

    public void deleteAppointment(Long id) {
        logger.info("Received request to delete appointment ID: {}", id);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        appointmentRepository.delete(appointment);
        logger.info("Successfully deleted appointment ID: {}", id);
    }

    @Transactional(readOnly = true)
    public List<AvailabilitySlotDto> getAvailability(LocalDate date) {
        logger.info("Calculating availability slots for date: {}", date);

        if (date == null) {
            throw new InvalidAppointmentException("Date parameter is required for checking availability");
        }

        List<Appointment> bookedAppointments = appointmentRepository.findByAppointmentDateAndStatusIn(date, ACTIVE_STATUSES);

        List<AvailabilitySlotDto> slots = new ArrayList<>();
        LocalTime current = DAY_START;

        while (current.plusMinutes(SLOT_DURATION_MINUTES).isBefore(DAY_END) ||
                current.plusMinutes(SLOT_DURATION_MINUTES).equals(DAY_END)) {
            LocalTime slotStart = current;
            LocalTime slotEnd = current.plusMinutes(SLOT_DURATION_MINUTES);

            boolean isBooked = bookedAppointments.stream().anyMatch(appointment ->
                    slotStart.isBefore(appointment.getEndTime()) && slotEnd.isAfter(appointment.getStartTime())
            );

            slots.add(new AvailabilitySlotDto(slotStart, slotEnd, isBooked ? "BOOKED" : "AVAILABLE"));
            current = slotEnd;
        }

        return slots;
    }

    private void validateAppointmentTime(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date == null) {
            throw new InvalidAppointmentException("Appointment date cannot be null");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new InvalidAppointmentException("Appointment date cannot be in the past");
        }
        if (startTime == null || endTime == null) {
            throw new InvalidAppointmentException("Start time and end time cannot be null");
        }
        if (!startTime.isBefore(endTime)) {
            throw new InvalidAppointmentException("Start time (" + startTime + ") must be strictly before end time (" + endTime + ")");
        }
    }

    private void checkTimeConflicts(LocalDate date, LocalTime startTime, LocalTime endTime, Long excludeId) {
        List<Appointment> conflicts;
        if (excludeId == null) {
            conflicts = appointmentRepository.findOverlappingAppointments(date, startTime, endTime, ACTIVE_STATUSES);
        } else {
            conflicts = appointmentRepository.findOverlappingAppointmentsExcludingId(date, startTime, endTime, ACTIVE_STATUSES, excludeId);
        }

        if (!conflicts.isEmpty()) {
            Appointment conflict = conflicts.get(0);
            logger.warn("Schedule conflict detected with existing appointment ID {}: {} - {}",
                    conflict.getId(), conflict.getStartTime(), conflict.getEndTime());
            throw new ScheduleConflictException(
                    String.format("Time slot [%s - %s] conflicts with existing appointment from %s to %s",
                            startTime, endTime, conflict.getStartTime(), conflict.getEndTime())
            );
        }
    }

    private AppointmentResponseDto mapToDto(Appointment appointment) {
        return new AppointmentResponseDto(
                appointment.getId(),
                appointment.getUser() != null ? appointment.getUser().getId() : null,
                appointment.getUser() != null ? appointment.getUser().getName() : null,
                appointment.getUser() != null ? appointment.getUser().getEmail() : null,
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }
}
