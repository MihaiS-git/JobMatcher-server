package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.PasswordResetToken;
import com.jobmatcher.server.domain.RefreshToken;
import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.EmailAlreadyExistsException;
import com.jobmatcher.server.exception.InvalidAuthException;
import com.jobmatcher.server.mapper.UserMapper;
import com.jobmatcher.server.model.*;
import com.jobmatcher.server.repository.PasswordResetTokenRepository;
import com.jobmatcher.server.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AuthenticationService {

    @Value("${frontend.base-url}")
    private String FRONTEND_BASE_URL;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final IRefreshTokenService refreshTokenService;
    private final IUserService userService;
    private final UserMapper userMapper;
    private final GmailSender gmailSender;
    private final PasswordResetTokenRepository tokenRepo;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            IRefreshTokenService refreshTokenService,
            IUserService userService, UserMapper userMapper, GmailSender gmailSender, PasswordResetTokenRepository tokenRepo
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.userMapper = userMapper;
        this.gmailSender = gmailSender;
        this.tokenRepo = tokenRepo;
    }

    public void register(RegisterRequest request) {
        log.info("User {} is attempting to register.", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use.");
        }

        try {
            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(Role.CUSTOMER);
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());

            User savedUser = userRepository.save(user);

            log.info("User {} registered successfully with role {}", savedUser.getEmail(), savedUser.getRole());
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
        return new AuthResponse(jwtToken, refreshToken.getToken(), userMapper.toDto(user));
    }

    @Transactional
    public boolean recoverPassword(RecoverPasswordRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isEmpty()) return false;

        User user = optionalUser.get();

        tokenRepo.deleteAllByUser(user);

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        tokenRepo.save(resetToken);

        String resetLink = FRONTEND_BASE_URL + "reset-password?token=" + token;

        String body = """
                    <p>Hello,</p>
                    <p>Click the following link to reset your JobMatcher password:</p>
                    <p><a href="https://jobmatcherclient.netlify.app/reset-password?token=%s">
                    Reset Password</a></p>
                    <p>If you didn't request this, please ignore.</p>
                    <p>Thanks,<br/>JobMatcher Team</p>
                """.formatted(token);

        try {
            gmailSender.sendEmail(user.getEmail(), "JobMatcher reset password", body);
        } catch (IOException | MessagingException e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
            tokenRepo.delete(resetToken);
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepo.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (token.isExpired()) {
            throw new IllegalArgumentException("Token expired");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        tokenRepo.deleteByToken(token.getToken());
    }

    public boolean validateResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepo.findByToken(token);
        return tokenOpt.isPresent() && !tokenOpt.get().isExpired();
    }
}
