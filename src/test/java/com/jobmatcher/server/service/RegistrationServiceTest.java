package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.EmailAlreadyExistsException;
import com.jobmatcher.server.model.RegisterRequest;
import com.jobmatcher.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    RegistrationService registrationService;

    @BeforeEach
    void setup() {
    }

    @Test
    void register_successfulRegistration() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registrationService.register(request);

        verify(userRepository).existsByEmail(request.getEmail());
        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_emailAlreadyExists_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicate@example.com");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        EmailAlreadyExistsException ex = assertThrows(EmailAlreadyExistsException.class,
                () -> registrationService.register(request));

        assertEquals("Email already in use.", ex.getMessage());

        verify(userRepository).existsByEmail(request.getEmail());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_dataIntegrityViolation_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test2@example.com");
        request.setPassword("password123");
        request.setFirstName("Jane");
        request.setLastName("Smith");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(DataIntegrityViolationException.class);

        EmailAlreadyExistsException ex = assertThrows(EmailAlreadyExistsException.class,
                () -> registrationService.register(request));

        assertEquals("Email already in use.", ex.getMessage());

        verify(userRepository).existsByEmail(request.getEmail());
        verify(passwordEncoder).encode(request.getPassword());
        verify(userRepository).save(any(User.class));
    }

}