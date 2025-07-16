package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.RefreshToken;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.model.AuthResponse;

import java.time.LocalDateTime;

public interface IRefreshTokenService {
    RefreshToken createRefreshToken(User user);

    int deleteAllExpiredSince(LocalDateTime now);

    void deleteByUser(User user);

    AuthResponse refreshToken(String refreshToken);
}
