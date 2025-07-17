package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.exception.ResourceNotFoundException;
import com.jobmatcher.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.jobmatcher.server.domain.User;


import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserServiceImpl userService;

    User sampleUser;

    @BeforeEach
    void setup(){
        sampleUser = new User();
        sampleUser.setEmail("test@example.com");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setEnabled(true);
        sampleUser.setAccountNonExpired(true);
        sampleUser.setAccountNonLocked(true);
        sampleUser.setCredentialsNonExpired(true);
        sampleUser.setRole(Role.valueOf("CUSTOMER"));
    }

    @Test
    void getUserByEmail_existingUser_returnsUser() {
        String email = "test@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(sampleUser));

        User result = userService.getUserByEmail(email);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);

        verify(userRepository).findByEmail(email);
    }

    @Test
    void getUserByEmail_userNotFound_throwsResourceNotFoundException() {
        String email = "notfound@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail(email))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findByEmail(email);
    }

}