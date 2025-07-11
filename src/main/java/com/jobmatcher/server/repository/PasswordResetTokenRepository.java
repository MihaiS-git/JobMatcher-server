package com.jobmatcher.server.repository;

import com.jobmatcher.server.domain.PasswordResetToken;
import com.jobmatcher.server.domain.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByToken(String token);

    @Modifying
    @Transactional
    @Query("delete from PasswordResetToken t where t.user = :user")
    void deleteAllByUser(User user);

    void deleteAllByExpiryDateBefore(LocalDateTime now);
}
