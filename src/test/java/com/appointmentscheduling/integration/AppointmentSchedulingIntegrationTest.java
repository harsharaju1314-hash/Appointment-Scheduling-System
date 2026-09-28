package com.appointmentscheduling.integration;

import com.appointmentscheduling.dto.*;
import com.appointmentscheduling.enums.AppointmentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AppointmentSchedulingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End Test: User Creation -> Appointment Booking -> Conflict -> Boundary Allowed -> Reschedule -> Cancel")
    void testFullAppointmentLifecycle() throws Exception {
        // 1. Create a new user
        UserRequestDto userRequest = new UserRequestDto("Dr. Evelyn Reed", "evelyn.reed@example.com");
        MvcResult userResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Dr. Evelyn Reed"))
                .andReturn();

        UserResponseDto createdUser = objectMapper.readValue(userResult.getResponse().getContentAsString(), UserResponseDto.class);
        Long userId = createdUser.getId();

        LocalDate appointmentDate = LocalDate.now().plusDays(5);

        // 2. Book an initial appointment (10:00 - 11:00)
        AppointmentRequestDto appt1 = new AppointmentRequestDto(userId, appointmentDate, LocalTime.of(10, 0), LocalTime.of(11, 0));
        MvcResult appt1Result = mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appt1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andReturn();

        AppointmentResponseDto createdAppt1 = objectMapper.readValue(appt1Result.getResponse().getContentAsString(), AppointmentResponseDto.class);
        Long appt1Id = createdAppt1.getId();

        // 3. Attempt overlapping booking (10:30 - 11:30) -> Should fail with 409 Conflict
        AppointmentRequestDto conflictAppt = new AppointmentRequestDto(userId, appointmentDate, LocalTime.of(10, 30), LocalTime.of(11, 30));
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictAppt)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));

        // 4. Book adjacent non-overlapping appointment at boundary (11:00 - 12:00) -> Should succeed with 201 Created
        AppointmentRequestDto adjacentAppt = new AppointmentRequestDto(userId, appointmentDate, LocalTime.of(11, 0), LocalTime.of(12, 0));
        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjacentAppt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startTime").value("11:00"))
                .andExpect(jsonPath("$.endTime").value("12:00"));

        // 5. Query availability for that date
        mockMvc.perform(get("/api/appointments/availability")
                        .param("date", appointmentDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 6. Reschedule Appt 1 to (14:00 - 15:00)
        RescheduleRequestDto reschedule = new RescheduleRequestDto(appointmentDate, LocalTime.of(14, 0), LocalTime.of(15, 0));
        mockMvc.perform(put("/api/appointments/" + appt1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("14:00"))
                .andExpect(jsonPath("$.endTime").value("15:00"));

        // 7. Cancel Appt 1
        mockMvc.perform(put("/api/appointments/" + appt1Id + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 8. Attempt to reschedule the cancelled appointment -> Should return 400 Bad Request
        RescheduleRequestDto invalidReschedule = new RescheduleRequestDto(appointmentDate, LocalTime.of(15, 0), LocalTime.of(16, 0));
        mockMvc.perform(put("/api/appointments/" + appt1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReschedule)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Cannot reschedule a cancelled appointment"));
    }
}
