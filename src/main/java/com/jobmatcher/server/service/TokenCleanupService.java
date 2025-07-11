package com.jobmatcher.server.service;

import com.jobmatcher.server.repository.PasswordResetTokenRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Transactional
@Service
public class TokenCleanupService {

    private final IRefreshTokenService refreshTokenService;
    private final PasswordResetTokenRepository tokenRepo;

    public TokenCleanupService(IRefreshTokenService refreshTokenService, PasswordResetTokenRepository tokenRepo) {
        this.refreshTokenService = refreshTokenService;
        this.tokenRepo = tokenRepo;
    }

    @Scheduled(fixedRate = 3600000)
    public void removeExpiredTokens(){
        int deleted = refreshTokenService.deleteAllExpiredSince(LocalDateTime.now(ZoneOffset.UTC));
        if (deleted > 0) {
            log.info("Deleted {} expired refresh tokens", deleted);
        } else {
            log.debug("No expired refresh tokens found");
        }
    }

    @Scheduled(cron = "0 0 3 * * ?") // Every day at 3 AM
    public void cleanUpExpiredPasswordRecoveryTokens() {
        tokenRepo.deleteAllByExpiryDateBefore(LocalDateTime.now());
    }
}
