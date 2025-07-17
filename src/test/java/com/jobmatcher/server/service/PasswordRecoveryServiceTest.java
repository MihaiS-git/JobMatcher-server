package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.PasswordResetToken;
import com.jobmatcher.server.domain.Role;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.EmailSendException;
import com.jobmatcher.server.exception.GmailApiException;
import com.jobmatcher.server.exception.PasswordResetException;
import com.jobmatcher.server.model.RecoverPasswordRequest;
import com.jobmatcher.server.model.ResetPasswordRequest;
import com.jobmatcher.server.repository.PasswordResetTokenRepository;
import com.jobmatcher.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordResetTokenRepository tokenRepo;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    GmailSender gmailSender;

    @InjectMocks
    PasswordRecoveryService service;

    User user;
    RecoverPasswordRequest recoverRequest;

    @BeforeEach
    void setup(){
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPassword("encodedPassword");
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setRole(Role.valueOf("CUSTOMER"));

        recoverRequest = new RecoverPasswordRequest();
        recoverRequest.setEmail(user.getEmail());
    }

    @Test
    void recoverPassword_returnsFalse_whenUserNotFound() {
        when(userRepository.lockUserByEmail(anyString())).thenReturn(Optional.empty());
        boolean result = service.recoverPassword(recoverRequest);
        assertThat(result).isFalse();
        verify(tokenRepo, never()).deleteAllByUser(any());
        verify(gmailSender, never()).sendResetEmail(any(), anyString());
    }

    @Test
    void recoverPassword_sendsEmailSuccessfully() {
        when(userRepository.lockUserByEmail(user.getEmail())).thenReturn(Optional.of(user));
        doNothing().when(gmailSender).sendResetEmail(any(), anyString());
        when(tokenRepo.save(any())).thenReturn(new PasswordResetToken());

        boolean result = service.recoverPassword(recoverRequest);

        assertThat(result).isTrue();
        verify(tokenRepo).deleteAllByUser(user);
        verify(gmailSender).sendResetEmail(eq(user), anyString());
    }

    @Test
    void recoverPassword_throwsEmailSendException_whenGmailApiException() {
        when(userRepository.lockUserByEmail(user.getEmail())).thenReturn(Optional.of(user));
        doThrow(new GmailApiException("Gmail failure", 500)).when(gmailSender).sendResetEmail(any(), anyString());
        when(tokenRepo.save(any())).thenReturn(new PasswordResetToken());

        assertThatThrownBy(() -> service.recoverPassword(recoverRequest))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("Gmail API error");

        verify(tokenRepo).deleteAllByUser(user);
    }

    @Test
    void recoverPassword_throwsEmailSendException_whenEmailSendException() {
        when(userRepository.lockUserByEmail(user.getEmail())).thenReturn(Optional.of(user));
        doThrow(new EmailSendException("Email failure", null)).when(gmailSender).sendResetEmail(any(), anyString());
        when(tokenRepo.save(any())).thenReturn(new PasswordResetToken());

        assertThatThrownBy(() -> service.recoverPassword(recoverRequest))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("Email failure");

        verify(tokenRepo).deleteAllByUser(user);
    }

    @Test
    void recoverPassword_throwsEmailSendException_whenOtherException() {
        when(userRepository.lockUserByEmail(user.getEmail())).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("Other failure")).when(gmailSender).sendResetEmail(any(), anyString());
        when(tokenRepo.save(any())).thenReturn(new PasswordResetToken());

        assertThatThrownBy(() -> service.recoverPassword(recoverRequest))
                .isInstanceOf(EmailSendException.class)
                .hasMessageContaining("Unexpected error");

        verify(tokenRepo).deleteAllByUser(user);
    }

    // resetPassword() tests

    @Test
    void resetPassword_successfulFlow() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("token");
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token");
        request.setPassword("newPass");

        when(tokenRepo.findByToken("token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPass")).thenReturn("encodedPass");
        when(userRepository.save(user)).thenReturn(user);

        service.resetPassword(request);

        assertThat(user.getPassword()).isEqualTo("encodedPass");
        verify(tokenRepo).deleteByToken("token");
    }

    @Test
    void resetPassword_throwsException_whenTokenNotFound() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("missing");

        when(tokenRepo.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    void resetPassword_throwsException_whenTokenExpired() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("token");
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().minusMinutes(1)); // expired

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("token");

        when(tokenRepo.findByToken("token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(PasswordResetException.class)
                .hasMessageContaining("Token expired");
    }

    // validateResetToken() tests

    @Test
    void validateResetToken_returnsTrue_forValidToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        when(tokenRepo.findByToken("token")).thenReturn(Optional.of(token));

        assertThat(service.validateResetToken("token")).isTrue();
    }

    @Test
    void validateResetToken_returnsFalse_forExpiredToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiryDate(LocalDateTime.now().minusMinutes(10));

        when(tokenRepo.findByToken("token")).thenReturn(Optional.of(token));

        assertThat(service.validateResetToken("token")).isFalse();
    }

    @Test
    void validateResetToken_returnsFalse_whenTokenMissing() {
        when(tokenRepo.findByToken("token")).thenReturn(Optional.empty());

        assertThat(service.validateResetToken("token")).isFalse();
    }

}