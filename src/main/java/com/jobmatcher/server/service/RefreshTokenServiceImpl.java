package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.RefreshToken;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.InvalidAuthException;
import com.jobmatcher.server.mapper.UserMapper;
import com.jobmatcher.server.model.AuthResponse;
import com.jobmatcher.server.model.AuthUserDTO;
import com.jobmatcher.server.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Transactional(readOnly = true)
@Slf4j
@Service
public class RefreshTokenServiceImpl implements IRefreshTokenService {

    @Value("${jwt.refresh.token.expiration}")
    private long refreshTokenExpirationTime;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            UserMapper userMapper
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        Optional<RefreshToken> existingTokenOpt = refreshTokenRepository.findByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(refreshTokenExpirationTime);

        if (existingTokenOpt.isPresent()) {
            RefreshToken existingToken = existingTokenOpt.get();
            existingToken.setToken(token);
            existingToken.setExpiryDate(expiryDate);
            return refreshTokenRepository.save(existingToken);
        } else {
            RefreshToken newToken = new RefreshToken();
            newToken.setToken(token);
            newToken.setUser(user);
            newToken.setExpiryDate(expiryDate);
            return refreshTokenRepository.save(newToken);
        }
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String oldRefreshToken) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(oldRefreshToken)
                .orElseThrow(() -> new InvalidAuthException("Refresh token not found"));

        if (oldToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.info("Refresh token expired at {}", oldToken.getExpiryDate());
            throw new InvalidAuthException("Invalid or expired refresh token");
        }

        User user = oldToken.getUser();

        refreshTokenRepository.delete(oldToken);

        String newAccessToken = jwtService.generateToken(user);
        RefreshToken newRefreshToken = createRefreshToken(user);

        AuthUserDTO userDTO = userMapper.toDto(user);

        return new AuthResponse(newAccessToken, newRefreshToken.getToken(), userDTO);
    }

    @Override
    @Transactional
    public int deleteAllExpiredSince(LocalDateTime now) {
        return refreshTokenRepository.deleteAllExpiredSince(now);
    }

    @Override
    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUserId(user.getId());
    }
}
