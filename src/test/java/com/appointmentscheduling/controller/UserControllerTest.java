package com.appointmentscheduling.controller;

import com.appointmentscheduling.dto.UserRequestDto;
import com.appointmentscheduling.dto.UserResponseDto;
import com.appointmentscheduling.exception.GlobalExceptionHandler;
import com.appointmentscheduling.exception.UserAlreadyExistsException;
import com.appointmentscheduling.exception.UserNotFoundException;
import com.appointmentscheduling.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/users should return 201 Created on valid input")
    void shouldCreateUser() throws Exception {
        UserRequestDto request = new UserRequestDto("John Doe", "john@example.com");
        UserResponseDto response = new UserResponseDto(1L, "John Doe", "john@example.com", LocalDateTime.now());

        when(userService.createUser(any(UserRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("POST /api/users should return 400 Bad Request when email is invalid")
    void shouldReturnBadRequestForInvalidEmail() throws Exception {
        UserRequestDto request = new UserRequestDto("John Doe", "invalid-email");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/users should return 409 Conflict when user email already exists")
    void shouldReturnConflictWhenEmailExists() throws Exception {
        UserRequestDto request = new UserRequestDto("John Doe", "john@example.com");

        when(userService.createUser(any(UserRequestDto.class)))
                .thenThrow(new UserAlreadyExistsException("User with email john@example.com already exists"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("User with email john@example.com already exists"));
    }

    @Test
    @DisplayName("GET /api/users/{id} should return 200 OK when user exists")
    void shouldGetUserById() throws Exception {
        UserResponseDto response = new UserResponseDto(1L, "John Doe", "john@example.com", LocalDateTime.now());
        when(userService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/users/{id} should return 404 Not Found when user does not exist")
    void shouldReturnNotFoundForMissingUser() throws Exception {
        when(userService.getUserById(999L)).thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/users should return list of users")
    void shouldGetAllUsers() throws Exception {
        UserResponseDto u1 = new UserResponseDto(1L, "John Doe", "john@example.com", LocalDateTime.now());
        UserResponseDto u2 = new UserResponseDto(2L, "Jane Doe", "jane@example.com", LocalDateTime.now());

        when(userService.getAllUsers()).thenReturn(Arrays.asList(u1, u2));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[1].name").value("Jane Doe"));
    }
}
