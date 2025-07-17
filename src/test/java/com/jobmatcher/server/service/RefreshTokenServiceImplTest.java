package com.jobmatcher.server.service;

import com.jobmatcher.server.domain.RefreshToken;
import com.jobmatcher.server.domain.User;
import com.jobmatcher.server.exception.InvalidAuthException;
import com.jobmatcher.server.mapper.UserMapper;
import com.jobmatcher.server.model.AuthResponse;
import com.jobmatcher.server.model.AuthUserDTO;
import com.jobmatcher.server.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    JwtService jwtService;

    @Mock
    UserMapper userMapper;

    @InjectMocks
    RefreshTokenServiceImpl refreshTokenService;

    private final long refreshTokenExpirationTime = 7; // days

    @BeforeEach
    void setup() throws IllegalAccessException, NoSuchFieldException {
        java.lang.reflect.Field field = RefreshTokenServiceImpl.class.getDeclaredField("refreshTokenExpirationTime");
        field.setAccessible(true);
        field.set(refreshTokenService, refreshTokenExpirationTime);
    }

    @Test
    void createRefreshToken_existingToken_updatedAndSaved() {
        User user = new User();
        user.setId(UUID.randomUUID());

        RefreshToken existingToken = new RefreshToken();
        existingToken.setToken("oldToken");
        existingToken.setUser(user);
        existingToken.setExpiryDate(LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByUserId(user.getId())).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertNotEquals("oldToken", result.getToken());
        assertTrue(result.getExpiryDate().isAfter(LocalDateTime.now()));

        verify(refreshTokenRepository).findByUserId(user.getId());
        verify(refreshTokenRepository).save(existingToken);
    }

    @Test
    void createRefreshToken_noExistingToken_createdAndSaved() {
        User user = new User();
        user.setId(UUID.randomUUID());

        when(refreshTokenRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertNotNull(result.getToken());
        assertTrue(result.getExpiryDate().isAfter(LocalDateTime.now()));

        verify(refreshTokenRepository).findByUserId(user.getId());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshToken_successfulFlow() {
        String oldTokenStr = "oldToken";
        User user = new User();
        user.setId(UUID.randomUUID());

        RefreshToken oldToken = new RefreshToken();
        oldToken.setToken(oldTokenStr);
        oldToken.setUser(user);
        oldToken.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken(oldTokenStr)).thenReturn(Optional.of(oldToken));
        doNothing().when(refreshTokenRepository).delete(oldToken);

        String newAccessToken = "newAccessToken";
        when(jwtService.generateToken(user)).thenReturn(newAccessToken);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setToken("newRefreshToken");

        // Spy refreshTokenService to verify createRefreshToken call
        RefreshTokenServiceImpl spyService = Mockito.spy(refreshTokenService);
        doReturn(newRefreshToken).when(spyService).createRefreshToken(user);

        AuthUserDTO userDTO = new AuthUserDTO();
        when(userMapper.toDto(user)).thenReturn(userDTO);

        AuthResponse response = spyService.refreshToken(oldTokenStr);

        assertEquals(newAccessToken, response.getToken());
        assertEquals(newRefreshToken.getToken(), response.getRefreshToken());
        assertEquals(userDTO, response.getUser());

        verify(refreshTokenRepository).findByToken(oldTokenStr);
        verify(refreshTokenRepository).delete(oldToken);
        verify(jwtService).generateToken(user);
        verify(spyService).createRefreshToken(user);
        verify(userMapper).toDto(user);
    }

    @Test
    void refreshToken_tokenNotFound_throwsException() {
        String oldTokenStr = "invalidToken";

        when(refreshTokenRepository.findByToken(oldTokenStr)).thenReturn(Optional.empty());

        InvalidAuthException ex = assertThrows(InvalidAuthException.class,
                () -> refreshTokenService.refreshToken(oldTokenStr));

        assertEquals("Refresh token not found", ex.getMessage());
    }

    @Test
    void refreshToken_expiredToken_throwsException() {
        String oldTokenStr = "oldToken";
        User user = new User();

        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken(oldTokenStr);
        expiredToken.setUser(user);
        expiredToken.setExpiryDate(LocalDateTime.now().minusDays(1)); // expired

        when(refreshTokenRepository.findByToken(oldTokenStr)).thenReturn(Optional.of(expiredToken));

        InvalidAuthException ex = assertThrows(InvalidAuthException.class,
                () -> refreshTokenService.refreshToken(oldTokenStr));

        assertEquals("Invalid or expired refresh token", ex.getMessage());
    }

    @Test
    void deleteAllExpiredSince_callsRepository() {
        LocalDateTime now = LocalDateTime.now();

        when(refreshTokenRepository.deleteAllExpiredSince(now)).thenReturn(5);

        int deletedCount = refreshTokenService.deleteAllExpiredSince(now);

        assertEquals(5, deletedCount);
        verify(refreshTokenRepository).deleteAllExpiredSince(now);
    }

    @Test
    void deleteByUser_callsRepository() {
        User user = new User();
        user.setId(UUID.randomUUID());

        doNothing().when(refreshTokenRepository).deleteByUserId(user.getId());

        refreshTokenService.deleteByUser(user);

        verify(refreshTokenRepository).deleteByUserId(user.getId());
    }
}