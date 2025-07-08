package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.RefreshToken;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenServiceImpl implements IRefreshTokenService{

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationTime;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public RefreshToken createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(refreshTokenExpirationTime);

        refreshTokenRepository.findByUserId(user.getId()).ifPresent(refreshTokenRepository::delete);

        RefreshToken newToken = new RefreshToken();
        newToken.setToken(token);
        newToken.setUser(user);
        newToken.setExpiryDate(expiryDate);

        return refreshTokenRepository.save(newToken);
    }

    @Override
    public int deleteAllExpiredSince(LocalDateTime now) {
        return refreshTokenRepository.deleteAllExpiredSince(now);
    }

    @Override
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());
    }
}
