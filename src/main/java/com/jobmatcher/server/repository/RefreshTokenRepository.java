package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByUserId(UUID id);

    @Query("SELECT r FROM RefreshToken r JOIN FETCH r.user WHERE r.token = :token")
    Optional<RefreshToken> findByToken(@Param("token") String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    int deleteAllExpiredSince(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);


}
