package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.RefreshToken;
import com.jobmatcher.server.domain.User;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IRefreshTokenService {
    RefreshToken createRefreshToken(User user);

    Optional<RefreshToken> findByToken(String token);

    public boolean validateRefreshToken(String token);

    int deleteAllExpiredSince(LocalDateTime now);

    void deleteByUser(User user);

    void deleteRefreshToken(String refreshToken);
}
