package com.appointmentscheduling.controller;

import com.appointmentscheduling.dto.AppointmentRequestDto;
import com.appointmentscheduling.dto.AppointmentResponseDto;
import com.appointmentscheduling.dto.AvailabilitySlotDto;
import com.appointmentscheduling.dto.RescheduleRequestDto;
import com.appointmentscheduling.enums.AppointmentStatus;
import com.appointmentscheduling.exception.AppointmentNotFoundException;
import com.appointmentscheduling.exception.GlobalExceptionHandler;
import com.appointmentscheduling.exception.ScheduleConflictException;
import com.appointmentscheduling.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppointmentController.class)
@Import(GlobalExceptionHandler.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/appointments should return 201 Created on valid request")
    void shouldCreateAppointment() throws Exception {
        LocalDate date = LocalDate.now().plusDays(3);
        AppointmentRequestDto request = new AppointmentRequestDto(1L, date, LocalTime.of(10, 0), LocalTime.of(10, 30));
        AppointmentResponseDto response = new AppointmentResponseDto(
                10L, 1L, "Alice", "alice@example.com", date, LocalTime.of(10, 0), LocalTime.of(10, 30),
                AppointmentStatus.BOOKED, LocalDateTime.now(), LocalDateTime.now()
        );

        when(appointmentService.createAppointment(any(AppointmentRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.startTime").value("10:00"))
                .andExpect(jsonPath("$.endTime").value("10:30"));
    }

    @Test
    @DisplayName("POST /api/appointments should return 409 Conflict when scheduling conflict exists")
    void shouldReturnConflictOnOverlappingBooking() throws Exception {
        LocalDate date = LocalDate.now().plusDays(3);
        AppointmentRequestDto request = new AppointmentRequestDto(1L, date, LocalTime.of(10, 15), LocalTime.of(10, 45));

        when(appointmentService.createAppointment(any(AppointmentRequestDto.class)))
                .thenThrow(new ScheduleConflictException("Time slot [10:15 - 10:45] conflicts with existing appointment from 10:00 to 10:30"));

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Time slot [10:15 - 10:45] conflicts with existing appointment from 10:00 to 10:30"));
    }

    @Test
    @DisplayName("GET /api/appointments/{id} should return 200 OK when found")
    void shouldGetAppointmentById() throws Exception {
        LocalDate date = LocalDate.now().plusDays(3);
        AppointmentResponseDto response = new AppointmentResponseDto(
                10L, 1L, "Alice", "alice@example.com", date, LocalTime.of(10, 0), LocalTime.of(10, 30),
                AppointmentStatus.BOOKED, LocalDateTime.now(), LocalDateTime.now()
        );

        when(appointmentService.getAppointmentById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/appointments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.userName").value("Alice"));
    }

    @Test
    @DisplayName("GET /api/appointments/{id} should return 404 Not Found when missing")
    void shouldReturnNotFoundForMissingAppointment() throws Exception {
        when(appointmentService.getAppointmentById(999L)).thenThrow(new AppointmentNotFoundException(999L));

        mockMvc.perform(get("/api/appointments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT /api/appointments/{id} should reschedule and return 200 OK")
    void shouldRescheduleAppointment() throws Exception {
        LocalDate newDate = LocalDate.now().plusDays(4);
        RescheduleRequestDto request = new RescheduleRequestDto(newDate, LocalTime.of(11, 0), LocalTime.of(11, 30));
        AppointmentResponseDto response = new AppointmentResponseDto(
                10L, 1L, "Alice", "alice@example.com", newDate, LocalTime.of(11, 0), LocalTime.of(11, 30),
                AppointmentStatus.BOOKED, LocalDateTime.now(), LocalDateTime.now()
        );

        when(appointmentService.rescheduleAppointment(eq(10L), any(RescheduleRequestDto.class))).thenReturn(response);

        mockMvc.perform(put("/api/appointments/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("11:00"))
                .andExpect(jsonPath("$.endTime").value("11:30"));
    }

    @Test
    @DisplayName("PUT /api/appointments/{id}/cancel should cancel appointment and return 200 OK")
    void shouldCancelAppointment() throws Exception {
        LocalDate date = LocalDate.now().plusDays(3);
        AppointmentResponseDto response = new AppointmentResponseDto(
                10L, 1L, "Alice", "alice@example.com", date, LocalTime.of(10, 0), LocalTime.of(10, 30),
                AppointmentStatus.CANCELLED, LocalDateTime.now(), LocalDateTime.now()
        );

        when(appointmentService.cancelAppointment(10L)).thenReturn(response);

        mockMvc.perform(put("/api/appointments/10/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("DELETE /api/appointments/{id} should delete appointment and return 204 No Content")
    void shouldDeleteAppointment() throws Exception {
        doNothing().when(appointmentService).deleteAppointment(10L);

        mockMvc.perform(delete("/api/appointments/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/appointments/availability should return list of slots for a given date")
    void shouldGetAvailabilitySlots() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);
        AvailabilitySlotDto slot1 = new AvailabilitySlotDto(LocalTime.of(9, 0), LocalTime.of(9, 30), "AVAILABLE");
        AvailabilitySlotDto slot2 = new AvailabilitySlotDto(LocalTime.of(9, 30), LocalTime.of(10, 0), "BOOKED");

        when(appointmentService.getAvailability(date)).thenReturn(Arrays.asList(slot1, slot2));

        mockMvc.perform(get("/api/appointments/availability")
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].startTime").value("09:00"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].startTime").value("09:30"))
                .andExpect(jsonPath("$[1].status").value("BOOKED"));
    }
}
