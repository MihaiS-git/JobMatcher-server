package com.jobmatcher.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
public class RefreshTokenCleanupService {

    private final IRefreshTokenService refreshTokenService;

    public RefreshTokenCleanupService(IRefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
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
}
