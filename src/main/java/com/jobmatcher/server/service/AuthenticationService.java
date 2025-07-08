package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.RefreshToken;
import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.EmailAlreadyExistsException;
import com.jobmatcher.server.exception.InvalidAuthException;
import com.jobmatcher.server.model.AuthResponse;
import com.jobmatcher.server.model.AuthenticationRequest;
import com.jobmatcher.server.model.RegisterRequest;
import com.jobmatcher.server.model.UserDTO;
import com.jobmatcher.server.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final IRefreshTokenService refreshTokenService;
    private final IUserService userService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            IRefreshTokenService refreshTokenService,
            IUserService userService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
    }

    public String register(RegisterRequest request) {
        log.info("User {} is attempting to register.", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use.");
        }

        try {
            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(Role.CUSTOMER);

            User savedUser = userRepository.save(user);

            log.info("User {} registered successfully with role {}", savedUser.getEmail(), savedUser.getRole());
            return jwtService.generateToken(savedUser);
        } catch (Exception e) {
            log.error("Error during registration: {}", e.getMessage());
            throw new InvalidAuthException("An error occurred during registration: " + e.getMessage());
        }
    }

    public String authenticate(AuthenticationRequest request) {
        log.info("User {} is attempting to authenticate.", request.getEmail());
        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new InvalidAuthException("Invalid email or password"));

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.warn("Bad credentials for email: {}", request.getEmail());
                throw new InvalidAuthException("Invalid email or password");
            }

            if (!user.isEnabled()) {
                log.warn("Account is disabled for email: {}", request.getEmail());
                throw new InvalidAuthException("Account is disabled");
            }

            if (!user.isAccountNonLocked()) {
                log.warn("Account is locked for email: {}", request.getEmail());
                throw new InvalidAuthException("Account is locked");
            }

            if (!user.isAccountNonExpired()) {
                log.warn("Account is expired for email: {}", request.getEmail());
                throw new InvalidAuthException("Account is expired");
            }

            if (!user.isCredentialsNonExpired()) {
                log.warn("Credentials are expired for email: {}", request.getEmail());
                throw new InvalidAuthException("Credentials are expired");
            }

            log.info("User {} authenticated successfully with role {}", user.getEmail(), user.getRole());
            return jwtService.generateToken(user);
        } catch (InvalidAuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            throw new InvalidAuthException("Authentication failed: " + e.getMessage());
        }
    }

    @Transactional
    public AuthResponse login(AuthenticationRequest request) {
        String jwtToken = authenticate(request);

        User user = userService.getUserByEmail(request.getEmail());

        refreshTokenService.deleteByUser(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        UserDTO userDto = UserDTO.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .pictureUrl(user.getPictureUrl())
                .build();

        return new AuthResponse(jwtToken, refreshToken.getToken(), userDto);
    }
}
