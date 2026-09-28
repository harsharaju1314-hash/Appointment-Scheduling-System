package com.appointmentscheduling.controller;

import com.appointmentscheduling.dto.AppointmentRequestDto;
import com.appointmentscheduling.dto.AppointmentResponseDto;
import com.appointmentscheduling.dto.AvailabilitySlotDto;
import com.appointmentscheduling.dto.RescheduleRequestDto;
import com.appointmentscheduling.enums.AppointmentStatus;
import com.appointmentscheduling.service.AppointmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDto> createAppointment(
            @Valid @RequestBody AppointmentRequestDto request) {
        logger.info("REST request to create appointment for user ID: {}", request.getUserId());
        AppointmentResponseDto response = appointmentService.createAppointment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDto> getAppointmentById(@PathVariable Long id) {
        logger.info("REST request to get appointment ID: {}", id);
        AppointmentResponseDto response = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDto>> getAllAppointments(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AppointmentStatus status) {
        logger.info("REST request to list appointments with filters - userId: {}, date: {}, status: {}", userId, date, status);
        List<AppointmentResponseDto> response = appointmentService.getAllAppointments(userId, date, status);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponseDto> rescheduleAppointment(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleRequestDto request) {
        logger.info("REST request to reschedule appointment ID: {}", id);
        AppointmentResponseDto response = appointmentService.rescheduleAppointment(id, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponseDto> cancelAppointment(@PathVariable Long id) {
        logger.info("REST request to cancel appointment ID: {}", id);
        AppointmentResponseDto response = appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
        logger.info("REST request to delete appointment ID: {}", id);
        appointmentService.deleteAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/availability")
    public ResponseEntity<List<AvailabilitySlotDto>> getAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        logger.info("REST request to get availability for date: {}", date);
        List<AvailabilitySlotDto> response = appointmentService.getAvailability(date);
        return ResponseEntity.ok(response);
    }
}
