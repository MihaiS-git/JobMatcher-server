package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.PasswordResetToken;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.EmailSendException;
import com.jobmatcher.server.exception.GmailApiException;
import com.jobmatcher.server.exception.PasswordResetException;
import com.jobmatcher.server.exception.TokenCreationException;
import com.jobmatcher.server.model.RecoverPasswordRequest;
import com.jobmatcher.server.model.ResetPasswordRequest;
import com.jobmatcher.server.repository.PasswordResetTokenRepository;
import com.jobmatcher.server.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final GmailSender gmailSender;

    public PasswordRecoveryService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepo,
            PasswordEncoder passwordEncoder,
            GmailSender gmailSender
    ) {
        this.userRepository = userRepository;
        this.tokenRepo = tokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.gmailSender = gmailSender;
    }

    @Transactional
    public boolean recoverPassword(RecoverPasswordRequest request) {
        User user = userRepository.lockUserByEmail(request.getEmail())
                .orElse(null);
        if (user == null) {
            return false;
        }

        tokenRepo.deleteAllByUser(user);
        String token = createAndSaveResetToken(user);

        try {
            gmailSender.sendResetEmail(user, token);
        } catch (GmailApiException e) {
            log.error("Failed to send reset email via Gmail API: {}", e.getMessage());
            throw new EmailSendException("Failed to send reset email due to Gmail API error.", e);
        } catch (EmailSendException e) {
            log.error("Email sending failure: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during password recovery email sending", e);
            throw new EmailSendException("Unexpected error during password recovery.", e);
        }

        return true;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepo.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (token.isExpired()) {
            throw new PasswordResetException("Token expired");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        tokenRepo.deleteByToken(token.getToken());
    }

    private String createAndSaveResetToken(User user) {
        int attempts = 0;
        while (attempts < 3) {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUser(user);
            resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
            try {
                tokenRepo.save(resetToken);
                return token;
            } catch (DataIntegrityViolationException e) {
                log.warn("Race condition detected. Retrying token creation for user {}", user.getId());
                tokenRepo.deleteAllByUser(user);
                attempts++;
            }
        }
        throw new TokenCreationException("Failed to create reset token after multiple attempts");
    }


    public boolean validateResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepo.findByToken(token);
        return tokenOpt.isPresent() && !tokenOpt.get().isExpired();
    }
}
