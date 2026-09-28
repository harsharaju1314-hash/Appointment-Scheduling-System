package com.appointmentscheduling.controller;

import com.appointmentscheduling.dto.UserRequestDto;
import com.appointmentscheduling.dto.UserResponseDto;
import com.appointmentscheduling.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto request) {
        logger.info("REST request to create user: {}", request.getEmail());
        UserResponseDto response = userService.createUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        logger.info("REST request to get user by ID: {}", id);
        UserResponseDto response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        logger.info("REST request to get all users");
        List<UserResponseDto> response = userService.getAllUsers();
        return ResponseEntity.ok(response);
    }
}
