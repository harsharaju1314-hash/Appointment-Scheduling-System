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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private AppointmentService appointmentService;

    private User sampleUser;
    private Appointment sampleAppointment;
    private LocalDate futureDate;

    @BeforeEach
    void setUp() {
        futureDate = LocalDate.now().plusDays(2);
        sampleUser = new User(1L, "Alice Johnson", "alice@example.com", LocalDateTime.now());
        sampleAppointment = new Appointment(
                100L,
                sampleUser,
                futureDate,
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                AppointmentStatus.BOOKED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Should successfully create appointment when no conflict exists")
    void shouldCreateAppointmentSuccessfully() {
        AppointmentRequestDto request = new AppointmentRequestDto(
                1L,
                futureDate,
                LocalTime.of(10, 0),
                LocalTime.of(10, 30)
        );

        when(userService.getUserEntityById(1L)).thenReturn(sampleUser);
        when(appointmentRepository.findOverlappingAppointments(eq(futureDate), eq(LocalTime.of(10, 0)), eq(LocalTime.of(10, 30)), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(sampleAppointment);

        AppointmentResponseDto response = appointmentService.createAppointment(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals(AppointmentStatus.BOOKED, response.getStatus());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should throw InvalidAppointmentException when start time is after or equal to end time")
    void shouldThrowExceptionWhenStartTimeIsAfterEndTime() {
        AppointmentRequestDto request = new AppointmentRequestDto(
                1L,
                futureDate,
                LocalTime.of(11, 0),
                LocalTime.of(10, 0)
        );

        InvalidAppointmentException exception = assertThrows(
                InvalidAppointmentException.class,
                () -> appointmentService.createAppointment(request)
        );

        assertTrue(exception.getMessage().contains("strictly before end time"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should throw InvalidAppointmentException when appointment date is in the past")
    void shouldThrowExceptionWhenAppointmentDateInPast() {
        AppointmentRequestDto request = new AppointmentRequestDto(
                1L,
                LocalDate.now().minusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30)
        );

        InvalidAppointmentException exception = assertThrows(
                InvalidAppointmentException.class,
                () -> appointmentService.createAppointment(request)
        );

        assertTrue(exception.getMessage().contains("cannot be in the past"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should throw ScheduleConflictException when overlapping appointment exists")
    void shouldThrowScheduleConflictExceptionOnOverlap() {
        AppointmentRequestDto request = new AppointmentRequestDto(
                1L,
                futureDate,
                LocalTime.of(10, 15),
                LocalTime.of(10, 45)
        );

        when(userService.getUserEntityById(1L)).thenReturn(sampleUser);
        when(appointmentRepository.findOverlappingAppointments(eq(futureDate), eq(LocalTime.of(10, 15)), eq(LocalTime.of(10, 45)), anyCollection()))
                .thenReturn(Collections.singletonList(sampleAppointment));

        ScheduleConflictException exception = assertThrows(
                ScheduleConflictException.class,
                () -> appointmentService.createAppointment(request)
        );

        assertTrue(exception.getMessage().contains("conflicts with existing appointment"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should allow adjacent non-overlapping appointment at exact boundary (e.g. 10:30-11:00 after 10:00-10:30)")
    void shouldAllowAdjacentAppointmentAtBoundary() {
        AppointmentRequestDto adjacentRequest = new AppointmentRequestDto(
                1L,
                futureDate,
                LocalTime.of(10, 30),
                LocalTime.of(11, 0)
        );

        Appointment adjacentAppointment = new Appointment(
                101L,
                sampleUser,
                futureDate,
                LocalTime.of(10, 30),
                LocalTime.of(11, 0),
                AppointmentStatus.BOOKED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userService.getUserEntityById(1L)).thenReturn(sampleUser);
        // Repository returns empty list because [10:00, 10:30) and [10:30, 11:00) do not overlap
        when(appointmentRepository.findOverlappingAppointments(eq(futureDate), eq(LocalTime.of(10, 30)), eq(LocalTime.of(11, 0)), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(adjacentAppointment);

        AppointmentResponseDto response = appointmentService.createAppointment(adjacentRequest);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals(LocalTime.of(10, 30), response.getStartTime());
        assertEquals(LocalTime.of(11, 0), response.getEndTime());
    }

    @Test
    @DisplayName("Should successfully reschedule appointment when new slot is available")
    void shouldRescheduleAppointmentSuccessfully() {
        RescheduleRequestDto request = new RescheduleRequestDto(
                futureDate,
                LocalTime.of(14, 0),
                LocalTime.of(14, 30)
        );

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(sampleAppointment));
        when(appointmentRepository.findOverlappingAppointmentsExcludingId(eq(futureDate), eq(LocalTime.of(14, 0)), eq(LocalTime.of(14, 30)), anyCollection(), eq(100L)))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(sampleAppointment);

        AppointmentResponseDto response = appointmentService.rescheduleAppointment(100L, request);

        assertNotNull(response);
        assertEquals(LocalTime.of(14, 0), sampleAppointment.getStartTime());
        assertEquals(LocalTime.of(14, 30), sampleAppointment.getEndTime());
        verify(appointmentRepository, times(1)).save(sampleAppointment);
    }

    @Test
    @DisplayName("Should throw ScheduleConflictException when rescheduling into a conflicting slot")
    void shouldThrowConflictWhenReschedulingIntoOccupiedSlot() {
        RescheduleRequestDto request = new RescheduleRequestDto(
                futureDate,
                LocalTime.of(11, 0),
                LocalTime.of(11, 30)
        );

        Appointment existingOther = new Appointment(
                102L,
                sampleUser,
                futureDate,
                LocalTime.of(11, 15),
                LocalTime.of(11, 45),
                AppointmentStatus.BOOKED,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(sampleAppointment));
        when(appointmentRepository.findOverlappingAppointmentsExcludingId(eq(futureDate), eq(LocalTime.of(11, 0)), eq(LocalTime.of(11, 30)), anyCollection(), eq(100L)))
                .thenReturn(Collections.singletonList(existingOther));

        assertThrows(ScheduleConflictException.class,
                () -> appointmentService.rescheduleAppointment(100L, request)
        );
        verify(appointmentRepository, never()).save(sampleAppointment);
    }

    @Test
    @DisplayName("Should throw InvalidAppointmentException when rescheduling a cancelled appointment")
    void shouldThrowExceptionWhenReschedulingCancelledAppointment() {
        sampleAppointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(sampleAppointment));

        RescheduleRequestDto request = new RescheduleRequestDto(
                futureDate,
                LocalTime.of(14, 0),
                LocalTime.of(14, 30)
        );

        InvalidAppointmentException exception = assertThrows(
                InvalidAppointmentException.class,
                () -> appointmentService.rescheduleAppointment(100L, request)
        );

        assertTrue(exception.getMessage().contains("Cannot reschedule a cancelled appointment"));
    }

    @Test
    @DisplayName("Should throw InvalidAppointmentException when rescheduling a completed appointment")
    void shouldThrowExceptionWhenReschedulingCompletedAppointment() {
        sampleAppointment.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(sampleAppointment));

        RescheduleRequestDto request = new RescheduleRequestDto(
                futureDate,
                LocalTime.of(14, 0),
                LocalTime.of(14, 30)
        );

        InvalidAppointmentException exception = assertThrows(
                InvalidAppointmentException.class,
                () -> appointmentService.rescheduleAppointment(100L, request)
        );

        assertTrue(exception.getMessage().contains("Cannot reschedule a completed appointment"));
    }

    @Test
    @DisplayName("Should successfully cancel an active appointment")
    void shouldCancelAppointmentSuccessfully() {
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(sampleAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(sampleAppointment);

        AppointmentResponseDto response = appointmentService.cancelAppointment(100L);

        assertNotNull(response);
        assertEquals(AppointmentStatus.CANCELLED, sampleAppointment.getStatus());
        verify(appointmentRepository, times(1)).save(sampleAppointment);
    }

    @Test
    @DisplayName("Should throw InvalidAppointmentException when cancelling an already cancelled appointment")
    void shouldThrowExceptionWhenCancellingAlreadyCancelledAppointment() {
        sampleAppointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(sampleAppointment));

        InvalidAppointmentException exception = assertThrows(
                InvalidAppointmentException.class,
                () -> appointmentService.cancelAppointment(100L)
        );

        assertTrue(exception.getMessage().contains("already cancelled"));
    }

    @Test
    @DisplayName("Should throw InvalidAppointmentException when cancelling a completed appointment")
    void shouldThrowExceptionWhenCancellingCompletedAppointment() {
        sampleAppointment.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(sampleAppointment));

        InvalidAppointmentException exception = assertThrows(
                InvalidAppointmentException.class,
                () -> appointmentService.cancelAppointment(100L)
        );

        assertTrue(exception.getMessage().contains("Cannot cancel a completed appointment"));
    }

    @Test
    @DisplayName("Should throw AppointmentNotFoundException when appointment does not exist")
    void shouldThrowNotFoundWhenAppointmentDoesNotExist() {
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AppointmentNotFoundException.class,
                () -> appointmentService.getAppointmentById(999L)
        );
    }

    @Test
    @DisplayName("Should correctly calculate availability slots for a day with bookings")
    void shouldCalculateAvailabilitySlotsCorrectly() {
        // Sample booking at 10:00 - 10:30
        when(appointmentRepository.findByAppointmentDateAndStatusIn(eq(futureDate), anyCollection()))
                .thenReturn(Collections.singletonList(sampleAppointment));

        List<AvailabilitySlotDto> slots = appointmentService.getAvailability(futureDate);

        assertNotNull(slots);
        assertFalse(slots.isEmpty());
        // Check slot 09:00 - 09:30 is AVAILABLE
        AvailabilitySlotDto slot9am = slots.stream()
                .filter(s -> s.getStartTime().equals(LocalTime.of(9, 0)))
                .findFirst()
                .orElse(null);
        assertNotNull(slot9am);
        assertEquals("AVAILABLE", slot9am.getStatus());

        // Check slot 10:00 - 10:30 is BOOKED
        AvailabilitySlotDto slot10am = slots.stream()
                .filter(s -> s.getStartTime().equals(LocalTime.of(10, 0)))
                .findFirst()
                .orElse(null);
        assertNotNull(slot10am);
        assertEquals("BOOKED", slot10am.getStatus());
    }
}
