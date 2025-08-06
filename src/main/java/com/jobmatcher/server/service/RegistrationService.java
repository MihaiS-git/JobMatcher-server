package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.EmailAlreadyExistsException;
import com.jobmatcher.server.model.RegisterRequest;
import com.jobmatcher.server.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequest request) {
        log.info("User {} is attempting to register.", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use.");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole());

        try {
            User savedUser = userRepository.save(user);
            log.info("User {} registered successfully with role {}", savedUser.getEmail(), savedUser.getRole());
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyExistsException("Email already in use.");
        }
    }
}
