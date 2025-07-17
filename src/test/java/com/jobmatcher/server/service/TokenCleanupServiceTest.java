package com.jobmatcher.server.service;

import com.jobmatcher.server.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenCleanupServiceTest {

    @Mock
    IRefreshTokenService refreshTokenService;

    @Mock
    PasswordResetTokenRepository tokenRepo;

    @InjectMocks
    TokenCleanupService tokenCleanupService;

    @BeforeEach
    void setup(){

    }

    @Test
    void removeExpiredTokens_deletesAndLogsWhenTokensDeleted() {
        when(refreshTokenService.deleteAllExpiredSince(any(LocalDateTime.class))).thenReturn(3);

        tokenCleanupService.removeExpiredTokens();

        verify(refreshTokenService).deleteAllExpiredSince(any(LocalDateTime.class));
    }

    @Test
    void removeExpiredTokens_logsWhenNoTokensDeleted() {
        when(refreshTokenService.deleteAllExpiredSince(any(LocalDateTime.class))).thenReturn(0);

        tokenCleanupService.removeExpiredTokens();

        verify(refreshTokenService).deleteAllExpiredSince(any(LocalDateTime.class));
    }

    @Test
    void cleanUpExpiredPasswordRecoveryTokens_callsRepoDelete() {
        tokenCleanupService.cleanUpExpiredPasswordRecoveryTokens();

        verify(tokenRepo).deleteAllByExpiryDateBefore(any(LocalDateTime.class));
    }
}