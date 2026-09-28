package com.appointmentscheduling.service;

import com.appointmentscheduling.dto.UserRequestDto;
import com.appointmentscheduling.dto.UserResponseDto;
import com.appointmentscheduling.entity.User;
import com.appointmentscheduling.exception.UserAlreadyExistsException;
import com.appointmentscheduling.exception.UserNotFoundException;
import com.appointmentscheduling.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private UserRequestDto userRequestDto;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "Alice Johnson", "alice@example.com", LocalDateTime.now());
        userRequestDto = new UserRequestDto("Alice Johnson", "alice@example.com");
    }

    @Test
    @DisplayName("Should successfully create a user when email is unique")
    void shouldCreateUserSuccessfully() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponseDto result = userService.createUser(userRequestDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Alice Johnson", result.getName());
        assertEquals("alice@example.com", result.getEmail());
        verify(userRepository, times(1)).existsByEmail("alice@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when creating user with duplicate email")
    void shouldThrowExceptionWhenUserEmailExists() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.createUser(userRequestDto)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(userRepository, times(1)).existsByEmail("alice@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should return user when user exists by ID")
    void shouldGetUserByIdSuccessfully() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponseDto result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Alice Johnson", result.getName());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user does not exist by ID")
    void shouldThrowUserNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(999L));
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should return all users")
    void shouldReturnAllUsers() {
        User user2 = new User(2L, "Bob Smith", "bob@example.com", LocalDateTime.now());
        when(userRepository.findAll()).thenReturn(Arrays.asList(sampleUser, user2));

        List<UserResponseDto> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals("Alice Johnson", result.get(0).getName());
        assertEquals("Bob Smith", result.get(1).getName());
        verify(userRepository, times(1)).findAll();
    }
}
